(ns liftbridge.grpc
  "Wrappers around java-grpc to sort of help?"
  (:import [io.netty.handler.ssl SslContextBuilder]
           [io.grpc.netty GrpcSslContexts]
           [java.io ByteArrayInputStream]
           [java.net InetSocketAddress]
           [io.grpc.netty NettyChannelBuilder]
           [io.grpc ManagedChannelBuilder NameResolver NameResolver$Factory
            NameResolver$Args NameResolver$Listener EquivalentAddressGroup Attributes])
  (:require [clojure.java.io :as io]))

(defn tls-config->ssl-context
  [{:keys [key-store-type key-cert-chain-file-path key-file-path key-password
           key-cert-chain key]}]
  (cond-> (GrpcSslContexts/forClient)
    key-store-type
    (.keyStoreType key-store-type)

    (and key-cert-chain-file-path key-file-path key-password)
    (.keyManager (io/as-file key-cert-chain-file-path)
                 (io/as-file key-file-path)
                 key-password)

    (and key-cert-chain-file-path key-file-path (not key-password))
    (.keyManager (io/as-file key-cert-chain-file-path)
                 (io/as-file key-file-path))

    (and key-cert-chain key key-password)
    (.keyManager (ByteArrayInputStream. (.getBytes key-cert-chain))
                 (ByteArrayInputStream. (.getBytes key))
                 key-password)

    (and key-cert-chain key (not key-password))
    (.keyManager (ByteArrayInputStream. (.getBytes key-cert-chain))
                 (ByteArrayInputStream. (.getBytes key)))

    :default (.build)))

(defn build-ssl-context [{:keys [tls-config ssl-context]}]
  (cond
    ssl-context ssl-context
    tls-config  (tls-config->ssl-context tls-config)))

(defn resolver-factory [brokers]
  (proxy [NameResolver$Factory] []
    (getDefaultScheme [_] "liftbridge")
    (newNameResolver [target-uri ^NameResolver$Args _args]
      (let [a                 (agent nil)
            refresh-addresses (fn [] (let [result (->> (shuffle brokers)
                                                      (map (fn [[host port]] (InetSocketAddress. host port)))
                                                      EquivalentAddressGroup.
                                                      list)]
                                      (send a
                                            #(.onAddresses % result Attributes/EMPTY))))]
        (proxy [NameResolver] []
          (getServiceAuthority [] (.getAuthority target-uri))
          (start [new-listener] (send a (constantly new-listener)) (refresh-addresses))
          (refresh []
            (refresh-addresses))
          (shutdown [_]))))))

(defn netty-channel
  [{:keys [max-conns-per-broker keep-alive-time
           tls-config ssl-context
           name-resolver-factory target
           resubscribe-wait-time override-authority
           brokers]
    :as   opts}]
  (let [ssl-context (build-ssl-context opts)]
    (cond-> (NettyChannelBuilder/forTarget target)
      brokers               (.nameResolverFactory (resolver-factory brokers))
      name-resolver-factory (.nameResolverFactory name-resolver-factory)
      override-authority    (.overrideAuthority override-authority)
      ssl-context           (.sslContext ssl-context)
      (not ssl-context)     .usePlaintext
      :default              (.build))))
