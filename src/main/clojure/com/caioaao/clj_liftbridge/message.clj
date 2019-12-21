(ns com.caioaao.clj-liftbridge.message
  (:import [liftbridge.proto Api$Message Api$AckPolicy]
           [java.util Date])
  (:require [clojure.instant :as instant]))

(defn assoc-when-not-empty [coll k v]
  (if (empty? v)
    coll
    (assoc coll k v)))

(defn ack-policy-from-obj [^Api$AckPolicy obj]
  (condp = obj
    Api$AckPolicy/LEADER :liftbridge.ack-policy/leader
    Api$AckPolicy/ALL    :liftbridge.ack-policy/all
    Api$AckPolicy/NONE   :liftbridge.ack-policy/none))

(defn from-object [^Api$Message obj]
  (-> {::offset          (.getOffset obj)
       ::key             (.toByteArray (.getKey obj))
       ::value           (.toByteArray (.getValue obj))
       ::timestamp-nanos (.getTimestamp obj)
       ::headers         (.getHeaders obj)
       ::ack-policy      (ack-policy-from-obj (.getAckPolicy obj))}
      (assoc-when-not-empty ::nat-reply-subject (.getReply obj))
      (assoc-when-not-empty ::nats-ack-inbox (.getAckInbox obj))
      (assoc-when-not-empty ::correlation-id (.getCorrelationId obj))))
