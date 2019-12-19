(ns liftbridge.client
  (:import [liftbridge.grpc.proto APIGrpc Api$CreateStreamRequest])
  (:require [clojure.java.io :as io]
            [liftbridge.grpc]))

(defprotocol IClient
  (create-stream [this opts])
  (subscribe [this subject]))

(defrecord Client [channel grpc-api]
  IClient
  (create-stream [this {:keys [subject name replication-factor group partitions] :as opts}]
    (cond-> (Api$CreateStreamRequest/newBuilder)
      replication-factor (.setReplicationFactor replication-factor)
      group              (.setGroup group)
      partitions         (.setPartitions partitions)
      :default           (-> (.setSubject subject)
                             (.setName name)
                             .build
                             (->> (.createStream grpc-api)))))
  java.io.Closeable
  (close [_this]
    (.shutdown channel)))

(defn connect
  "Attempts to connect to a Liftbridge server with multiple options"
  [& {:keys [grpc-channel] :as opts}]
  (let [channel (or grpc-channel (liftbridge.grpc/netty-channel opts))]
    (->Client channel (APIGrpc/newBlockingStub channel))))

(comment
  (def client  (connect :brokers [["localhost" 9292]]
                        :target "liftbridge://localhost"))

  (def stream-res (create-stream client "banana"))

  )
