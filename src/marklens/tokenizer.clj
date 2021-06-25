(ns marklens.tokenizer
  (:require [clojure.string :as str]))

(defn deaccent
  "Remove accent from string. Reference: https://gist.github.com/maio/e5f85d69c3f6ca281ccd"
  [str]
  (let [normalized (java.text.Normalizer/normalize str java.text.Normalizer$Form/NFD)]
    (str/replace normalized #"\p{InCombiningDiacriticalMarks}+" "")))

(defn exclude-bad-tokens
  [tokens forbidden-tokens]
  (filter
   #(and (not (str/blank? (:term %)))
         (not (contains? forbidden-tokens (:term %))))
   tokens))

(defn set-index
  [tokens]
  (first
   (reduce
    (fn [[tokens-with-index index]
         token]
      [(conj tokens-with-index {:term token :index index})
       (+ index 1 (count token))]) ; this >1< regards the lost caracter used to split string
    [[] 0]
    tokens)))

(defn tokenize
  [text stop-words]
  (exclude-bad-tokens
   (set-index
    (->
     text
     (deaccent)
     (str/lower-case)
     (str/replace #"[^a-zA-Z0-9]" " ")
     (str/split #" ")))
   stop-words))
