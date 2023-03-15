(ns objectifier-client.cli
  (:require [objectifier-client.core :as obj]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]))

(defn- read-file-bytes [filename]
  (with-open [in  (java.io.FileInputStream. filename)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(def cli-opts
  [["-s" "--server SERVER" "Hostname of the Objectifier server."]
   ["-p" "--port PORT"     "Port on which the Objectifier server is listening."
    :default 80
    :parse-fn #(Integer/parseInt %)]
   ["-l" "--labels"        "Only print detected labels."]
   ["-h" "--help"          "Print this message."]
   ["-v" "--verbose"       "Provide verbose output."]])

(defn- msg-quit [status msg]
  (println msg)
  (System/exit status))

(defn- usage
  ([summary] (usage summary []))
  ([summary errors] (->> (concat errors
                                 ["usage: objectifier-client [opts] <filename>"
                                  ""
                                  "Options:"
                                  summary])
                         (str/join \newline))))

(defn- display-probabilities [client filename]
  (let [probabilities (obj/get-probabilities! client (read-file-bytes filename))]
    (println (str filename ":"))
    (doseq [[lbl prob] probabilities]
      (println (format "  %s - %.2f"
                       (name lbl)
                       prob)))))

(defn- display-labels [client filenames]
  (distinct
   (mapcat (fn [filename]
             (obj/get-labels! client (read-file-bytes filename)))
           filenames)))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]}]
    (when (seq errors)
      (msg-quit 1 (usage summary errors)))
    (when (:help options)
      (msg-quit 0 (usage summary)))
    (when (empty? (arguments))
      (msg-quit 0 (usage summary ["No files provided to scan."])))
    (let [client (obj/define-connection "http" (:server options) (:port options))]
      (if (:labels options)
        (display-labels client arguments)
        (doseq [file arguments]
          (display-probabilities client file))))))
