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
  (get-probabilities! [_ image-data]))

(defn- url->string [url] (.toExternalForm url))

(defn- build-url [{:keys [scheme host port]}]
  (url->string (URL. scheme host port "/images")))

(defn- send-image! [url image-bytes]
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

(defn- pthru [o] (pprint o) o)

(defrecord ObjectifierClient [scheme host port]
  IObjectifierClient
  (get! [self image-data]
    (process-response (send-image! (build-url (pthru self)) image-data)))

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
          (get-detections! self image-data))))

(defn define-connection
  [& {:keys [scheme host port]
      :or   {scheme "http"
             port   80}}]
  (->ObjectifierClient scheme host port))
