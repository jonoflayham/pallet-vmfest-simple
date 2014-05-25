(defproject pallet-vmfest-triage "0.1.0-SNAPSHOT"
    :description "Test harness to solve a few problems using Pallet and VMFest"
    :dependencies [[org.clojure/clojure "1.6.0"]
                   [com.palletops/pallet "0.8.0-RC.9"]
                   [com.palletops/pallet-vmfest "0.4.0-alpha.1"]
                   [org.clojars.tbatchelli/vboxjws "4.3.4"]
                   [ch.qos.logback/logback-classic "1.0.9"]
                   [org.clojure/tools.namespace "0.2.4"]
                   ]
    :local-repo-classpath true
    :leiningen/reply {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.2"]]
                      :exclusions [commons-logging]}
    :jvm-opts ["-Dhttp.nonProxyHosts=10.153.*"])
