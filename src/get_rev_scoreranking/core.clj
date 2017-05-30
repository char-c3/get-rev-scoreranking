(ns get-rev-scoreranking.core
  (:require [net.cgrand.enlive-html :as eh]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:use [clojure.pprint :only [pprint]]))

(def page-count 10)

(def highscore-ranking-uri "http://www.capcom.co.jp/arcade/rev/PC/ranking_highscore.html")

(defn scrape [uri]
  (with-open [r (io/reader uri)]
    (eh/html-resource r)))

;;(def scraped-data (scrape highscore-ranking-uri))

;;(def music-list (doall (eh/select scraped-data #{[:div.rkHiscoreCv]})))

(defn uri->music-list [uri]
  (doall (eh/select (scrape uri) #{[:div.rkHiscoreCv]})))

(defn get-music-title [music]
  (first (:content (first (eh/select music #{[:p.rkHiName]})))))

(defn get-uri-list [music]
  (let [uri-list (doall (eh/select music #{[:ul :li :a]}))
        re #"q=\w+"]
    (-> #(subs (->> %
             (:attrs)
             (:href)
             (:apply str)
             (re-find re)) 2)
        (map uri-list))))

(defn padding-uri-list [uri-list]
  (if (< (count uri-list) 4)
    (padding-uri-list (concat [""] uri-list))
    uri-list))

(defn music-list->map [music-list]
  (loop [l music-list ret []]
    (if (empty? l) ret
      (let [music (first l)
            title (get-music-title music)
            uri   (padding-uri-list (get-uri-list music))]
        (recur (rest l) (conj ret {:title title :uri uri}))))))

(defn -main [& args]
  (binding [*out* (java.io.FileWriter. "rank-uri-list.json")]
    (json/pprint 
      (loop [i 1 ret []]
        (if (<= i page-count)
          (recur (inc i) (concat ret (-> (str highscore-ranking-uri "?page=" i)
                                         (uri->music-list)
                                         (music-list->map))))
          ret)))
    (flush)))

