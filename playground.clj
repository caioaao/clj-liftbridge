
;; (comment
;;   (import '[io.grpc ManagedChannelBuilder])
;;   (def chan (->> (ManagedChannelBuilder/forAddress "localhost" 9292)
;;                    .usePlaintext
;;                    .build))

;;   (def client (connect chan))
;;   @(create-stream client "one-stream" {})

;;   (def subs-chan (subscribe client "one-stream" {}))
;;   @(publish client "one-stream" (.getBytes "testing"))

;;   (def msg (async/<!! subs-chan))
;;   (do @(publish client "one-stream" (.getBytes "testing"))
;;       (async/<!! subs-chan))

;;   (.shutdown chan)

;;   )
