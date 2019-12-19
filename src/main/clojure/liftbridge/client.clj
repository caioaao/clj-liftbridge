(ns liftbridge.client
  (:import [liftbridge.proto APIGrpc Api$CreateStreamRequest
            Api$SubscribeRequest Api$SubscribeRequest$Builder
            Api$StartPosition Api$Message APIGrpc$APIStub
            APIGrpc$APIBlockingStub ]
           [io.grpc.stub StreamObserver])
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]))

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

  (subscribe
    [this stream-name {:keys [partition-number start-at
                              start-at-offset start-at-time
                              start-at-time-delta handler-fn]
                       :as   opts}]
    "Subscribes to a stream identified by `stream-name`. Returns a core.async
    channel where messages are published. Options are:

     - `partition-number` Specifies the stream partition to consume. Defaults to
    `0`

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

    - `async-chan` Channel to be used instead of `(clojure.core.async/chan)`.

    Currently, Subscribe can only subscribe to a single partition. In the
    future, there will be functionality for consuming all partitions.

    If an error occurs during consumption, the throwable object is published to
    the channel and the channel is closed.")

  (publish [this subject message {:keys [] :as opts}]
    "Publishes `message` to the NATS `subject`."))

(defn- set-subscription-starting-point
  [^Api$SubscribeRequest$Builder
   subscribe-request-builder
   start-at {:keys [start-offset start-time]}]
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
        (.setStartTimestamp (.getTime ^java.util.Date start-time)))

    nil subscribe-request-builder))

(defn- chan->observer ^StreamObserver [chan]
  (reify StreamObserver
    (onNext [_ v]
      (async/>!! chan v))
    (onError [_ ex]
      (async/>!! chan ex)
      (async/close! chan))
    (onCompleted [_]
      (async/close! chan))))

(defn- async-call-chan [f & channel]
  (let [channel (or channel (async/chan))]
    (f (chan->observer channel))
    channel))

(defn- async-call-future [f]
  (future (let [res (async/<!! (async-call-chan f))]
            (if (instance? Throwable res)
              (throw res)
              res))))

(defrecord Client [channel async-stub]
  IClient
  (create-stream
    [this stream-name {:keys [subject replication-factor group partitions]}]
    (let [req-obj (cond-> (Api$CreateStreamRequest/newBuilder)
                    replication-factor (.setReplicationFactor replication-factor)
                    group              (.setGroup group)
                    partitions         (.setPartitions partitions)
                    :default           (-> (.setName stream-name)
                                           (.setSubject (or subject stream-name))
                                           .build))]
      (async-call-future #(.createStream ^APIGrpc$APIStub async-stub
                                         req-obj
                                         ^StreamObserver %))))

  (subscribe [this stream-name {:keys [partition-number start-at
                                       start-offset start-time
                                       handler-fn async-chan]
                                :as opts}]
    (let [request-obj (cond-> (Api$SubscribeRequest/newBuilder)
                        partition-number (.setPartition partition-number)
                        start-at         ^Api$SubscribeRequest$Builder (set-subscription-starting-point start-at opts)
                        :default         (-> (.setStream stream-name)
                                             .build))]
      (async-call-chan #(.subscribe ^APIGrpc$APIStub async-stub
                                    ^Api$SubscribeRequest request-obj
                                    %)))))

(defn connect
  "Attempts to connect to a Liftbridge server with multiple options"
  [grpc-channel]
  (->Client grpc-channel (APIGrpc/newStub grpc-channel)))

(comment
  (import '[io.grpc ManagedChannelBuilder])
  (def chan (->> (ManagedChannelBuilder/forAddress "localhost" 9292)
                   .usePlaintext
                   .build))
  (def client (connect chan))
  @(create-stream client "one-stream" {})


  (subscribe client "one-stream" {:handler-fn (fn [& args] (prn args))})
  )
