(defproject marklens "2.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/test.check "1.1.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [com.mchange/c3p0 "0.9.5.5"]

                 ; fixes dependency problems in clj-tagsoup
                 [org.clojure/data.xml "0.0.8"]
                 [clj-tagsoup/clj-tagsoup "0.3.0" :exclusions
                    [org.clojure/clojure org.clojure/data.xml]]

                 [clojure.java-time "0.3.2"]]
  :main marklens.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
