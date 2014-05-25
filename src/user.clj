(ns user
  (:require [pallet.crate.automated-admin-user :only [authorize-user-key]]
            [pallet.compute :only [instantiate-provider]]
            [pallet.compute.vmfest :only [add-image has-image?]]
            [pallet.configure :only [admin-user compute-service]]
            [pallet.actions :only [user] :refer [exec-script* directory remote-file rsync-directory]]
            [pallet.crate :only [admin-user defplan]]
            [pallet.crate.ssh-key :only ssh-key]
            [pallet.crate.sudoers :only sudoers]
            ; And because this project's aimed purely at REPL exploration...
            [pallet.repl :as prepl]
            [clojure.repl :refer (doc source)]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.pprint :refer [pp pprint]]))

(def hardware-models {:small-hardware {:memory-size 512 :cpu-count 1 :network-type :local}})

(def small-node (pallet.api/node-spec
                  :image {:image-id "centos-dual-nw-interface"}
                  ; Using :hardware-id instead of :hardware tickles
                  ; https://github.com/pallet/pallet-vmfest/issues/17
                  :hardware hardware-models))

(pallet.crate/defplan add-admin-user
                      "A reduced version of automated-admin-user without overloading and
                      in particular without requiring forbidden yum invocation - environment
                      is locked down"
                      ([]
                       (let [user-to-add (pallet.crate/admin-user)
                             username (:username user-to-add)
                             public-key-path (:public-key-path user-to-add)]

                         (clojure.tools.logging/debugf "Adding admin user for %s" user-to-add)
                         (pallet.actions/user username :create-home true :shell :bash)
                         (pallet.crate.ssh-key/authorize-key username (slurp public-key-path))
                         (pallet.crate.sudoers/sudoers
                           {}
                           {:default {:env_keep "SSH_AUTH_SOCK"}}
                           {username {:ALL {:run-as-user :ALL :tags :NOPASSWD}}}))))

(def small-group (pallet.api/group-spec "small-group"
                                        :node-spec small-node
                                        :phases {:bootstrap add-admin-user}))

(print "Initialising VMFest compute service provider")

(declare compute-service-provider)

(defn init []
  (def compute-service-provider (pallet.compute/instantiate-provider "vmfest"
                                                                     :vbox-comm :ws
                                                                     ; URL specified here redundant, but highlights
                                                                     ; our intention to use a Pallet-remote VirtualBox
                                                                     ; once everything's working locally.
                                                                     :url "http://localhost:18083"
                                                                     :hardware-models hardware-models)))

(def guest-image-path "/Users/jon/_vm/boxen/centos-dual-nw-interface/centos-dual-nw-interface.vdi")

(defn ensure-image []
  (when-not (pallet.compute.vmfest/has-image? compute-service-provider :centos-dual-nw-interface)
    (pallet.compute.vmfest/add-image compute-service-provider guest-image-path)))

(defn up []
  (pallet.api/converge {small-group 1} :compute compute-service-provider))

(defn down []
  (pallet.api/converge {small-group 0} :compute compute-service-provider))

(print "\n\n(init) before anything else, then (ensure-image), (up) and (down)")