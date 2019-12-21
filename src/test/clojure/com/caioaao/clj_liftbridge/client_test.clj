(ns com.caioaao.clj-liftbridge.client-test
  (:import [io.grpc ManagedChannelBuilder])
  (:require [com.caioaao.clj-liftbridge.client :as clj-liftbridge.client]
            [state-flow.cljtest :as cljtest :refer [defflow]]
            [state-flow.labs.cljtest :as lab.cljtest :refer [testing]]
            [state-flow.state :as state]
            [clojure.test :refer [is]]
            [clojure.core.async :as async]
            [com.caioaao.clj-liftbridge.message :as message]))

(def server-ip "localhost")
(def server-port 9292)

(defn init! [& _]
  {:grpc-channel (->> (ManagedChannelBuilder/forAddress server-ip server-port)
                      .usePlaintext
                      .build)})

(defn cleanup! [{:keys [grpc-channel]}]
  (.shutdown grpc-channel))

(defn random-stream-name []
  (apply str (take 10 (repeatedly #(char (+ (rand 26) 65))))))

(defflow stream-creation
  {:init init!
   :cleanup cleanup!}
  [lift-conn (state/gets (comp clj-liftbridge.client/connect :grpc-channel))
   stream-name (state/wrap-fn (fn [] (random-stream-name)))
   stream-creation-res (state/wrap-fn #(clj-liftbridge.client/create-stream lift-conn
                                                                            stream-name
                                                                            {}))]

  (testing "stream creating returns a future"
    (is @stream-creation-res))

  [stream-creation-retry (state/wrap-fn #(clj-liftbridge.client/create-stream lift-conn
                                                                              stream-name
                                                                              {}))]
  (testing "creation request is idempotent and second try fails"
    (is (thrown-with-msg? java.util.concurrent.ExecutionException
                          #"ALREADY_EXISTS: partition already exists"
                          @stream-creation-retry))))

(defflow publish-and-subscirbe
  {:init    init!
   :cleanup cleanup!}
  [lift-conn (state/gets (comp lift-connect :grpc-channel))
   stream-name (state/wrap-fn (fn [] (random-stream-name)))]
  (state/wrap-fn #(deref (clj-liftbridge.client/create-stream lift-conn stream-name {})))

  [subscription-chan (state/wrap-fn #(clj-liftbridge.client/subscribe lift-conn
                                                                      stream-name
                                                                      {}))]

  (testing "first received message is empty"
    (is (match? #::message{:offset          0
                           :key             bytes?
                           :value           bytes?
                           :timestamp-nanos 0}
                (async/<!! subscription-chan))))

  (testing "we can publish bytes to one stream"
    (is @(clj-liftbridge.client/publish lift-conn stream-name
                                        (.getBytes "hello world"))))

  (testing "subscription channel receives the message"
    (is (match? #::message{:offset          int?
                           :key             bytes?
                           :value           #(-> % String. (= "hello world"))
                           :timestamp-nanos int?}
                (async/<!! subscription-chan)))))
