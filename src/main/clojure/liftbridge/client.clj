(ns liftbridge.client
  (:import [liftbridge.proto APIGrpc Api$CreateStreamRequest
            Api$SubscribeRequest Api$StartPosition])
  (:require [clojure.java.io :as io]))

(defprotocol IClient
  (create-stream
    [this stream-name {:keys [subject replication-factor group partitions]
                       :as opts}]
    "Creates a stream. Arguments:

     - `stream-name` A name string which uniquely identifies the stream in the
       Liftbridge cluster. Attempting to create another stream with the same
       name will result in an error.

     - `subject` A subject string which is the NATS subject to attach the stream
       to. If the stream has more than one partition, this is used as the base
       subject for each partition. If the subject is not set, it will use
       `stream-name` instead.

     Other supported optional arguments are `replication-factor`,
     `max-replication`, `group`, and `partitions`. Refer to the official
     Liftbridge documentation for more information on this options.")

  (subscribe [this stream-name {:keys [partition-number start-at
                                       start-at-offset start-at-time
                                       start-at-time-delta handler-fn]
                                :as opts}]
    "Subscribes to a stream identified by `stream-name`. If `handler-fn` is not
    provided, returns a core.async channel where messages are published. Options
    are:

     - `partition-number` Specifies the stream partition to consume. Defaults to
    `0`

     - `handler-fn` Function to be called for every published message.

     - `start-at` Sets the start position. Can be:

       - `:liftbridge.start-at/new-only` Sets the subscription start position to
         new messages received in the stream.

       - `:liftbridge.start-at/earliest-received` Sets the subscription start
         position to the earliest message received in the stream.

       - `:liftbridge.start-at/latest-received` Sets the subscription start
         position to the last message received in the stream.

       - `:liftbridge.start-at/offset` Uses `start-offset` to set start
         position.

       - `:liftbridge.start-at/time` Uses `start-time` option to set start
         position.

       - `:liftbridge.start-at/time-delta` Uses `start-time-delta` option to set
         start position.

    - `start-offset` Sets the subscription start position to the first
      message with an offset greater than or equal to the given offset.

    - `start-time` Sets the subscription start position to the first message
      with a timestamp greater than or equal to the given instant.

    Currently, Subscribe can only subscribe to a single partition. In the
    future, there will be functionality for consuming all partitions.")

  (publish [this subject message {:keys [] :as opts}]
    "Publishes `message` to the NATS `subject`."))

(defn- set-subscription-starting-point [subscribe-request-builder start-at
                                        {:keys [start-offset start-time]}]
  (case start-at
    :liftbridge.start-at/new-only
    (.setStartPosition subscribe-request-builder
                       Api$StartPosition/NEW_ONLY)

    :liftbridge.start-at/earliest-received
    (.setStartPosition subscribe-request-builder
                       Api$StartPosition/EARLIEST)

    :liftbridge.start-at/latest-received
    (.setStartPosition subscribe-request-builder
                       Api$StartPosition/LATEST)

    :liftbridge.start-at/offset
    (-> subscribe-request-builder
        (.setStartPosition Api$StartPosition/OFFSET)
        (.setStartOffset start-offset))

    :liftbridge.start-at/time
    (-> subscribe-request-builder
        (.setStartPosition Api$StartPosition/TIMESTAMP)
        (.setStartTimestamp (.getTime start-time)))

    nil subscribe-request-builder))

(defrecord Client [channel api-stub]
  IClient
  (create-stream
    [this stream-name {:keys [subject replication-factor group partitions
                              max-replication]}]
    (cond-> (Api$CreateStreamRequest/newBuilder)
      replication-factor (.setReplicationFactor replication-factor)
      group              (.setGroup group)
      partitions         (.setPartitions partitions)
      max-replication    (.setMaxReplication max-replication)
      :default           (-> (.setName stream-name)
                             (.setSubject (or subject stream-name))
                             .build
                             (->> (.createStream api-stub)))))

  (subscribe [this stream-name {:keys [partition-number start-at
                                       start-offset start-time
                                       handler-fn]
                                :as opts}]
    (let [handler-fn (fn [& args] #p args)
          request-obj (cond-> (Api$SubscribeRequest/newBuilder)
                        partition-number (.setPartition partition-number)
                        start-at         (set-subscription-starting-point start-at opts)
                        :default         (-> (.setStream stream-name)
                                             .build))]
      (.subscribe api-stub request-obj handler-fn)))

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
  (create-stream client "one-stream" {})


  (subscribe client "one-stream" {})
  )
