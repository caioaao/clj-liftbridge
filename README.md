# clj-liftbridge [![Clojars Project](https://img.shields.io/clojars/v/com.caioaao/clj-liftbridge.svg)](https://clojars.org/com.caioaao/clj-liftbridge) [![cljdoc badge](https://cljdoc.org/badge/com.caioaao/clj-liftbridge)](https://cljdoc.org/d/com.caioaao/clj-liftbridge/CURRENT)

**This project is under development**

Clojure client for [Liftbridge](https://github.com/liftbridge-io/liftbridge), a system that provides lightweight, fault-tolerant message streams for [NATS](https://nats.io).

Liftbridge provides the following high-level features:

- Log-based API for NATS
- Replicated for fault-tolerance
- Horizontally scalable
- Wildcard subscription support
- At-least-once delivery support and message replay
- Message key-value support
- Log compaction by key

## Basic Usage

```clojure
(require '[com.caioaao.clj-liftbridge.client :as lift])
(import '[io.grpc ManagedChannelBuilder])

;; create the gRPC channel to connect to Liftbridge API
(def grpc-channel (->> (ManagedChannelBuilder/forAddress server-ip server-port)
                       .usePlaintext
                       .build))

;; Create  Liftbridge client.
(def lift-client (lift/connect grpc-channel))

;; Create a stream named `foo`
@(lift/create-stream lift-client "foo" {})

;; publish a message to "foo-stream"
(lift/publish lift-client "foo" (.getBytes "hello"))

;; subscribe to the stream starting from the beginning.
(def ch (lift/subscribe lift-client "foo" {:start-at :liftbridge.start-at/earliest-received}))

;; get messages from the channel returned by the subscription function
(require '[clojure.core.async :refer [<!!]])
(println (<!! ch))

;; cleanup
(.shutdown grpc-channel)
```

For more information, see the [API docs](https://cljdoc.org/badge/com.caioaao/clj-liftbridge/CURRENT).

## TODO list

- [ ] Improve test coverage
- [ ] `publish` high level API (on hold - see liftbridge-io/liftbridge-api#10)
- [ ] Improve connection (or remove attrition caused by the need for a Java class)
