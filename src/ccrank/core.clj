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
        global-rating (->> ratings (map f) (reduce +))
        rating-count (count ratings)]
    (assoc team 
      :rating-count rating-count
      :global-rating
      (if (pos? rating-count)
        (float (/ global-rating rating-count))
        0))))

(defn -main
  []
  (let [n     (atom 1)
        teams (transit->json teams)
        teams-details (->> teams
                        (map team-details)
                        (map total-rating)
                        doall)]
    (println "# Public Favorite Ranking")
    (println)
    (println "| # | Team | Votes |")
    (println "|---|------|-------|")
    (doseq [team-detail (->> teams-details 
                          (sort-by :faver-count)
                          reverse)]
      (println "|" @n
               "|" (:team/app-name team-detail)
               "|" (:faver-count team-detail)
               "|")
      (swap! n inc))
    (reset! n 1)
    (println "# Judges Ranking")
    (println)
    (println "| # | Team | Rating Count | Ratings Avg. |")
    (println "|---|------|--------------|--------------|")
    (doseq [team-detail (->> teams-details
                          (sort-by :global-rating)
                          reverse)]
      (println "|" @n
               "|" (:team/app-name team-detail)
               "|" (:rating-count team-detail)
               "|" (:global-rating team-detail)
               "|")
      (swap! n inc))))




