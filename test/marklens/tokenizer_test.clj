(ns marklens.tokenizer-test
  (:require [marklens.tokenizer :as tokenizer]
            [clojure.test :refer [deftest is]]))

(deftest ignores-multiple-whitespaces
  (is (=
       (list
        {:term "test" :index 0}
        {:term "test" :index 8}
        {:term "test" :index 13})
       (tokenizer/tokenize "test    test test" #{}))))

(deftest ignores-newlines
  (is (=
       (list
        {:term "test" :index 0}
        {:term "test" :index 8}
        {:term "test" :index 16})
       (tokenizer/tokenize "test    test \n\n\rtest" #{}))))

(deftest special-characters-are-whitespaces
  (is (=
       (list
        {:term "test" :index 0}
        {:term "asdf" :index 6}
        {:term "as" :index 28})
       (tokenizer/tokenize "test''asdf<>/@#$%&**(@*()@#$as" #{}))))
