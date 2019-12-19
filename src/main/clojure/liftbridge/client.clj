(ns liftbridge.client
  (:import [liftbridge.proto APIGrpc Api$CreateStreamRequest])
  (:require [clojure.java.io :as io]))

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
  [grpc-channel]
  (->Client grpc-channel (APIGrpc/newBlockingStub grpc-channel)))

(comment
  (import '[io.grpc ManagedChannelBuilder])
  (def client (->> (ManagedChannelBuilder/forAddress "localhost" 9292)
                   .usePlaintext
                   .build
                   connect))
  (create-stream client {:subject "one-stream"
                         :name    "one-stream"})
  )
