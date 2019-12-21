(ns com.caioaao.clj-liftbridge.client-test
  (:import [io.grpc ManagedChannelBuilder])
  (:require [com.caioaao.clj-liftbridge.client :as clj-liftbridge.client]
            [state-flow.cljtest :as cljtest :refer [defflow]]
            [state-flow.labs.cljtest :as lab.cljtest :refer [testing]]
            [state-flow.state :as state]
            [clojure.test :refer [is]]))

(def server-ip "localhost")
(def server-port 9292)

(defn init! [& _]
  {:grpc-channel (->> (ManagedChannelBuilder/forAddress server-ip server-port)
                      .usePlaintext
                      .build)})

(defn cleanup! [{:keys [grpc-channel]}]
  (.shutdown grpc-channel))

(defn lift-connect [grpc-channel]
  (clj-liftbridge.client/connect grpc-channel))

(defn random-stream-name []
  (apply str (take 10 (repeatedly #(char (+ (rand 26) 65))))))

(defn create-stream! [lift-connection stream-name opts]
  (-> lift-connection
      (clj-liftbridge.client/create-stream stream-name opts)))

(defflow stream-creation
  {:init init!
   :cleanup cleanup!}
  [lift-conn (state/gets (comp lift-connect :grpc-channel))
   random-str (state/wrap-fn (fn [] (random-stream-name)))
   stream-creation-res (state/wrap-fn #(create-stream! lift-conn random-str {}))]

  (testing "stream creating returns a future that evaluates to nil when succeeds"
    (is (nil? @stream-creation-res)))

  [stream-creation-retry (state/wrap-fn #(create-stream! lift-conn random-str {}))]
  (testing "creation request is idempotent and second try fails"
    (is (thrown-with-msg? java.util.concurrent.ExecutionException
                          #"ALREADY_EXISTS: partition already exists"
                          @stream-creation-retry))))

(defflow publish-and-subscirbe
  {:init    init!
   :cleanup cleanup!}
  [lift-conn (state/gets (comp lift-connect :grpc-channel))
   random-str (state/wrap-fn (fn [] (random-stream-name)))]

  (state/wrap-fn #(create-stream! lift-conn random-str {})))
