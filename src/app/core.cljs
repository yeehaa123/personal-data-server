(ns app.core
  (:require [cljs.core.async :refer [<! close! put! chan >!]]
            [cljs.nodejs :as node])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def ^:private AWS (node/require "aws-sdk"))
(def ^:private fs (node/require "fs"))
(def js-links (.parse js/JSON (.readFileSync fs "./urls.json", "utf8")))
(def links (filter :url (js->clj js-links :keywordize-keys true)))

(node/enable-util-print!)
(def Kinesis (new AWS.Kinesis))
(def params {:ShardCount 1
             :StreamName "tweeted-bookmarks"})
(def link-count (atom 0))

(defn create-stream []
  (let [c (chan)]
    (.createStream Kinesis (clj->js params) #(if %1
                                          (println "error "%1)
                                          (go (>! c (js->clj %2 :keywordize-keys true)))))
    c))

(defn create-message [link]
  {:Data (.stringify js/JSON (clj->js link))
   :StreamName "tweeted-bookmarks"
   :PartitionKey "user"})

(defn send-message [msg]
  (.putRecord Kinesis (clj->js msg) #(if %1
                                       (println %1)
                                       (println %2))))

(defn -main []
  (go
    (doseq [link links]
      (let [msg (create-message link)]
        (send-message msg)))))

(set! *main-cli-fn* -main)

