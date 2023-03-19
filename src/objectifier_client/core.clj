(ns objectifier-client.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [slingshot.slingshot :refer [throw+]]
            [clojure.pprint :refer [pprint]])
  (:import java.net.URL
           java.io.ByteArrayInputStream))

(defprotocol IObjectifierClient
  (get!               [_ image-data])
  (get-labels!        [_ image-data])
  (get-detections!    [_ image-data])
  (get-highlights!    [_ image-data])
  (get-probabilities! [_ image-data])
  (get-summary!       [_ image-data]))

(defn- url->string [url] (.toExternalForm url))

(defn- build-url [{:keys [scheme host port]}]
  (url->string (URL. scheme host port "/images")))

(defn- send-image! [verbose url image-bytes]
  (when verbose
    (println (str "sending "
                  (count image-bytes)
                  " to "
                  url
                  " for object detection")))
  (let [input-stream (ByteArrayInputStream. image-bytes)]
    (client/post url
                 {:multipart [{:name "image"
                               :content input-stream}]})))

(defn- to-keyword [str]
  (-> str
      (str/lower-case)
      (str/replace #" " "-")
      (keyword)))

(defn- process-response [resp]
  (if (<= 200 (:status resp) 299)
    (-> resp :body (json/read-str :key-fn to-keyword))
    (throw+ {:type     ::http-error
             :status   (:status resp)
             :reason   (:reason-phrase resp)
             :response resp})))

(defrecord ObjectifierClient [scheme host port verbose]
  IObjectifierClient
  (get! [self image-data]
    (process-response (send-image! verbose (build-url self) image-data)))

  (get-labels! [self image-data]
    (-> (get! self image-data)
        :labels))

  (get-detections! [self image-data]
    (-> (get! self image-data)
        :detections))

  (get-highlights! [self image-data]
    (-> (get! self image-data)
        :output))

  (get-probabilities! [self image-data]
    (into {}
          (map (juxt (comp keyword :label) :confidence))
          (get-detections! self image-data)))

  (get-summary! [self image-data]
    (let [result (get! self image-data)]
      {:output  (:output result)
       :objects (into {}
                      (map (juxt (comp keyword :label) :confidence))
                      (get-detections! self image-data))})))

(defn define-connection
  [& {:keys [scheme  host port verbose]
      :or   {scheme  "http"
             port    80
             verbose false}}]
  (->ObjectifierClient scheme host port verbose))
