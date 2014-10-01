(ns ccrank.core
  (:require [cognitect.transit :as transit]
            [clojure.pprint :as pp])
  (:import [java.io StringReader ByteArrayInputStream]))

(def teams (slurp "teams"))

(def teams-url "https://backend.clojurecup.com/teams")
(def team-url "https://backend.clojurecup.com/teams/%s")

(defn transit->json
  [transit-str]
  (let [in (ByteArrayInputStream. (.getBytes transit-str))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn url->json 
  [url]
  (println "Getting " url)
  (-> url slurp transit->json))

(defn- team-details
  [team]
  (->> (:team/app-domain team)
    (format team-url )
    url->json))

(defn total-rating
  [team]
  (let [ratings (:ratings team)
        f       (fn [{:keys [utility design completeness innovation]}]
                  (/ (reduce + [utility design completeness innovation]) 4))
        global-rating (->> ratings (map f) (reduce +))]
    (assoc team :global-rating 
      (if (pos? (count ratings))
        (float (/ global-rating (count ratings)))
        0))))

(defn -main
  []
  (let [teams (transit->json teams)
        teams-details (->> teams
                        (map team-details)
                        (map total-rating)
                        doall)]
    (println "===== Public Favorite Ranking =====")
    (doseq [team-detail (->> teams-details 
                          (sort-by :faver-count)
                          reverse)]
      (println
        (:team/app-name team-detail)
        (:faver-count team-detail)))
    (println "===== Judges Ranking =====")
    (doseq [team-detail (->> teams-details
                          (sort-by :global-rating)
                          reverse)]
      (println
        (:team/app-name team-detail)
        (:global-rating team-detail)))))
