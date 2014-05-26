pallet-vmfest-simple
====================

A simple pallet-vmfest project for problem-solving.

The stack:

* leiningen 2.3.4
* pallet 0.8.0-RC.9
* pallet-vmfest 0.4.0-alpha.1
* clojure 1.6.0
* VirtualBox 4.3.12
* Java 7u55
* MacOS 10.9.3

The current problem: can't converge nodes down to 0
---------------------------------------------------------------------------------

We've created a CentOS 6.5 vdi multi-attach image with two DHCPed network interfaces and not much else, following the approach at eg http://pepijndevos.nl/2013/03/20/vmfest-base-image.html.  We're tied to using CentOS 6.5 for various reasons.

We can converge up to 1 node, and everything works beautifully.

``` bash
$ lein repl
```

``` clojure
(init)
(up)
```

But when we converge down to 0 nodes...

``` clojure
(down)
```

... pallet-vmfest is told that VirtualBox has received an 'invalid managed object reference', to an object with an id of the form xxxxxxxxxxxxxxxx-0000000000000xxx, and the rest of the operation fails.  Same thing happens with Clojure 1.5.1 and/or VirtualBox 4.3.4.

Log output (also [as a gist](https://gist.github.com/jonoflayham/9774cf714c9049af2ac0)):

```
2014-05-25 18:48:28,359 ERROR [operate-20] v.v.conditions Cannot parse the error since the object is unavailable org.virtualbox_4_3.VBoxException: VirtualBox error: Invalid managed object reference "523cfd7b52707877-0000000000000427"
2014-05-25 18:48:18,992 DEBUG [operate-19] p.c.operations lift :phases [:configure] :targets [:small-group]
2014-05-25 18:48:18,994 DEBUG [operate-20] p.c.primitives build-and-execute-phase :configure on 1 target(s)
2014-05-25 18:48:27,955 DEBUG [operate-19] p.c.operations converge :phase [:configure] :groups [:small-group] :settings-groups [:small-group]
2014-05-25 18:48:28,047 DEBUG [operate-19] p.c.primitives build-and-execute-phase :destroy-server on 1 target(s)
2014-05-25 18:48:28,108 DEBUG [operate-19] p.c.primitives remove-group-nodes {{:image {:image-id "centos-dual-nw-interface"}, :count 0, :phases {:pallet/os #<clojure.lang.AFunction$1@5d99ae19>, :pallet/os-bs #<clojure.lang.AFunction$1@14751b51>, :bootstrap #<clojure.lang.AFunction$1@1364679d>}, :group-name :small-group, :default-phases [:configure], :hardware {:small-hardware {:cpu-count 1, :network-type :local, :memory-size 512}}} {:nodes ({:hardware {:small-hardware {:cpu-count 1, :network-type :local, :memory-size 512}}, :default-phases [:configure], :group-name :small-group, :phases {:pallet/os #<clojure.lang.AFunction$1@5d99ae19>, :pallet/os-bs #<clojure.lang.AFunction$1@14751b51>, :bootstrap #<clojure.lang.AFunction$1@1364679d>}, :count 0, :image {:image-id "centos-dual-nw-interface"}, :group-names #{:small-group}, :node  small-group-0	    small-group	 public: 192.168.56.101}), :all true}}
2014-05-25 18:48:28,152 DEBUG [operate-20] p.c.primitives remove-nodes {:nodes ({:hardware {:small-hardware {:cpu-count 1, :network-type :local, :memory-size 512}}, :default-phases [:configure], :group-name :small-group, :phases {:pallet/os #<clojure.lang.AFunction$1@5d99ae19>, :pallet/os-bs #<clojure.lang.AFunction$1@14751b51>, :bootstrap #<clojure.lang.AFunction$1@1364679d>}, :count 0, :image {:image-id "centos-dual-nw-interface"}, :group-names #{:small-group}, :node  small-group-0	    small-group	 public: 192.168.56.101}), :all true}
2014-05-25 18:48:28,154 DEBUG [operate-20] p.core.api remove-nodes
2014-05-25 18:48:28,367 WARN [operate-20] p.c.primitives async-fsm failed
clojure.lang.ExceptionInfo: throw+: {:message "Cannot open session with machine '7080ff54-e0fc-43dd-8a77-6a856df9601d' reason:VirtualBox error: rc=0x80070005 The object is not ready (0x80070005)", :full-message "Cannot open session with machine '7080ff54-e0fc-43dd-8a77-6a856df9601d' reason:VirtualBox error: rc=0x80070005 The object is not ready (0x80070005): VirtualBox error: rc=0x80070005 The object is not ready (0x80070005)", :cause #<VBoxException org.virtualbox_4_3.VBoxException: VirtualBox error: rc=0x80070005 The object is not ready (0x80070005)>}
	at vmfest.virtualbox.conditions$wrap_exception.invoke(conditions.clj:132) ~[na:na]
	at vmfest.manager$power_down$fn__19133.invoke(manager.clj:542) ~[na:na]
	at vmfest.manager$power_down.invoke(manager.clj:542) ~[na:na]
	at pallet.compute.vmfest.service$node_shutdown.invoke(service.clj:661) ~[na:na]
	at pallet.compute.vmfest.service$node_destroy.invoke(service.clj:669) ~[na:na]
	at pallet.compute.vmfest.service.VmfestService.destroy_nodes_in_group(service.clj:864) ~[na:na]
	at pallet.core.api$remove_nodes.invoke(api.clj:316) ~[na:na]
	at clojure.lang.AFn.applyToHelper(AFn.java:160) [clojure-1.6.0.jar:na]
	at clojure.lang.AFn.applyTo(AFn.java:144) [clojure-1.6.0.jar:na]
	at clojure.core$apply.invoke(core.clj:630) ~[clojure-1.6.0.jar:na]
	at clojure.core$partial$fn__4232.doInvoke(core.clj:2472) ~[clojure-1.6.0.jar:na]
	at clojure.lang.RestFn.invoke(RestFn.java:397) ~[clojure-1.6.0.jar:na]
	at pallet.core.primitives$async_fsm$run_async__11649$async_fsm__11651.invoke(primitives.clj:42) ~[na:na]
	at pallet.algo.fsmop$report_exceptions$report_exceptions__6886.invoke(fsmop.clj:64) [na:na]
	at clojure.lang.AFn.call(AFn.java:18) [clojure-1.6.0.jar:na]
	at java.util.concurrent.FutureTask.run(FutureTask.java:262) [na:1.7.0_55]
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145) [na:1.7.0_55]
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615) [na:1.7.0_55]
	at java.lang.Thread.run(Thread.java:745) [na:1.7.0_55]
```