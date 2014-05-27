pallet-vmfest-simple
====================

A simple pallet-vmfest project for problem-solving.

The stack:

* leiningen 2.3.4
* pallet 0.8.0-RC.9
* pallet-vmfest 0.4.0-alpha.1
* clojure 1.6.0 or 1.5.1
* VirtualBox 4.3.12 or 4.3.4
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

But when we converge down to 0 nodes in the same REPL session...

``` clojure
(down)  ; i.e.  (pallet.api/converge {small-group 0} :compute compute-service-provider))
```

then...

* an exception is thrown.  pallet-vmfest is told that VirtualBox has received an 'invalid managed object reference', to an object with an id of the form xxxxxxxxxxxxxxxx-0000000000000xxx
* that id is always the last in a sequence of 7 or 8 such ids which appear in the vboxwebsrv logs for this operation.  It's as if something takes it one id too far!
* the machine is powered off, but not deleted
* a subsequent convergence to 1 node creates a new instance alongside the old one, named the same
* logs are as shown below.

We see this behaviour 75% of the time.  Does that mean it's a race condition?  The other 25%, everything works like a dream: the machine is powered down and deleted just fine, and there are no errors.  It seems that if we execute the same steps but use a new REPL session for the converge to zero, the problem happens more often, but that's just an impression.

Log output from Pallet (also [as a gist](https://gist.github.com/jonoflayham/9774cf714c9049af2ac0)):

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

Log output from verbose vboxwebsrv follows.  This is for a different run exhibiting the same problem.  Search for '''196f7''', which is the offending managed object reference. 

'''
From Pallet:

user=> (pallet.api/converge {small-centos-group 0} :compute @compute-service-provider)
18:40:29.442 [operate-21] ERROR vmfest.virtualbox.conditions - Cannot parse the error since the object is unavailable org.virtualbox_4_3.VBoxException: VirtualBox error: Invalid managed object reference "bc3f7732304b19b4-00000000000196f7"
18:40:29.448 [operate-21] WARN  pallet.core.primitives - async-fsm failed
clojure.lang.ExceptionInfo: throw+: {:message "Cannot open session with machine 'a294397b-d2f9-4fc8-b6a6-bbae171183e9' reason:VirtualBox error: rc=0x80070005 The object is not ready (0x80070005)", :full-message "Cannot open session with machine 'a294397b-d2f9-4fc8-b6a6-bbae171183e9' reason:VirtualBox error: rc=0x80070005 The object is not ready (0x80070005): VirtualBox error: rc=0x80070005 The object is not ready (0x80070005)", :ca.......

From vboxwebsrv, from the start of the converge-down operation; mostly preamble, so search for 196f7 to get close to the error:

07:20:55.220005 SQPmp    Request 2335 on socket 8 queued for processing (1 items on Q)
07:20:55.220040 SQW02    Processing connection from IP=127.0.0.1 socket=8 (1 out of 2 threads idle)
07:20:55.220431 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.220728 main     Pumping COM event queue
07:20:55.220740 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000102603880 (IUnknown*=0x00000102603880; COM refcount now 3/4), new ID is 19654; now 1 objects total
07:20:55.220748 SQW02       * authenticate: created session object with comptr 0x00000102603880, MOR = 249ae145867cc955-0000000000019654
07:20:55.220827 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19655; now 2 objects total
07:20:55.220836 SQW02    VirtualBox object ref is 249ae145867cc955-0000000000019655
07:20:55.220840 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.221772 SQW02    -- entering __vbox__IVirtualBox_USCOREgetMachinesByGroups
07:20:55.221778 SQW02       findRefFromId(): looking up objref 249ae145867cc955-0000000000019655
07:20:55.221783 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.221787 SQW02       calling COM method GetMachinesByGroups
07:20:55.221979 SQW02       done calling COM method
07:20:55.221991 main     Pumping COM event queue
07:20:55.221995 SQW02       convert COM output "returnval" back to caller format
07:20:55.222158 main     Pumping COM event queue
07:20:55.222170 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102601280 (IUnknown*=0x0000010220d3b0; COM refcount now 3/3), new ID is 19656; now 3 objects total
07:20:55.222178 SQW02       done converting COM output "returnval" back to caller format
07:20:55.222182 SQW02    -- leaving __vbox__IVirtualBox_USCOREgetMachinesByGroups, rc: 0x0 (0)
07:20:55.222862 SQW02    -- entering __vbox__IMachine_USCOREgetId
07:20:55.222868 SQW02       findRefFromId(): looking up objref 249ae145867cc955-0000000000019656
07:20:55.222873 SQW02       findComPtrFromId(): returning original IMachine*=0x102601280 (IUnknown*=0x10220D3B0)
07:20:55.222875 SQW02       calling COM method COMGETTER(Id)
07:20:55.223022 SQW02       done calling COM method
07:20:55.223031 SQW02       convert COM output "id" back to caller format
07:20:55.223038 main     Pumping COM event queue
07:20:55.223056 SQW02       done converting COM output "id" back to caller format
07:20:55.223061 SQW02    -- leaving __vbox__IMachine_USCOREgetId, rc: 0x0 (0)
07:20:55.223938 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.223954 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19654 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.223959 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19655 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.224107 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19656 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.224114 SQW02    session destroyed, 0 sessions left open
07:20:55.224116 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.226749 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.226950 main     Pumping COM event queue
07:20:55.226969 SQW02       * ManagedObjectRef: MOR created for ISession*=0x000001025008a0 (IUnknown*=0x000001025008a0; COM refcount now 3/4), new ID is 19657; now 1 objects total
07:20:55.226976 SQW02       * authenticate: created session object with comptr 0x000001025008a0, MOR = c39d0d8fde1614e0-0000000000019657
07:20:55.227082 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19658; now 2 objects total
07:20:55.227091 SQW02    VirtualBox object ref is c39d0d8fde1614e0-0000000000019658
07:20:55.227094 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.227791 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.227798 SQW02       findRefFromId(): looking up objref c39d0d8fde1614e0-0000000000019658
07:20:55.227813 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.227818 SQW02       calling COM method FindMachine
07:20:55.227990 SQW02       done calling COM method
07:20:55.228002 main     Pumping COM event queue
07:20:55.228016 SQW02       convert COM output "returnval" back to caller format
07:20:55.228198 main     Pumping COM event queue
07:20:55.228221 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100700 (IUnknown*=0x00000102604c60; COM refcount now 3/2), new ID is 19659; now 3 objects total
07:20:55.228237 SQW02       done converting COM output "returnval" back to caller format
07:20:55.228240 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.228823 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.228828 SQW02       findRefFromId(): looking up objref c39d0d8fde1614e0-0000000000019659
07:20:55.228832 SQW02       findComPtrFromId(): returning original IMachine*=0x106100700 (IUnknown*=0x102604C60)
07:20:55.228834 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.228995 SQW02       done calling COM method
07:20:55.229002 SQW02       convert COM output "accessible" back to caller format
07:20:55.229008 SQW02       done converting COM output "accessible" back to caller format
07:20:55.229013 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.229018 main     Pumping COM event queue
07:20:55.229893 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.229909 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19657 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.229914 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19658 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.230028 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19659 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.230035 SQW02    session destroyed, 0 sessions left open
07:20:55.230038 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.231509 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.231676 main     Pumping COM event queue
07:20:55.231685 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106100050 (IUnknown*=0x00000106100050; COM refcount now 3/4), new ID is 1965A; now 1 objects total
07:20:55.231692 SQW02       * authenticate: created session object with comptr 0x00000106100050, MOR = c41b624951009a17-000000000001965a
07:20:55.231761 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1965B; now 2 objects total
07:20:55.231769 SQW02    VirtualBox object ref is c41b624951009a17-000000000001965b
07:20:55.231772 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.232255 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.232262 SQW02       findRefFromId(): looking up objref c41b624951009a17-000000000001965b
07:20:55.232266 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.232270 SQW02       calling COM method FindMachine
07:20:55.232453 SQW02       done calling COM method
07:20:55.232459 main     Pumping COM event queue
07:20:55.232470 SQW02       convert COM output "returnval" back to caller format
07:20:55.232589 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100220 (IUnknown*=0x0000010220cdd0; COM refcount now 3/2), new ID is 1965C; now 3 objects total
07:20:55.232596 main     Pumping COM event queue
07:20:55.232635 SQW02       done converting COM output "returnval" back to caller format
07:20:55.232645 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.233162 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.233167 SQW02       findRefFromId(): looking up objref c41b624951009a17-000000000001965c
07:20:55.233182 SQW02       findComPtrFromId(): returning original IMachine*=0x106100220 (IUnknown*=0x10220CDD0)
07:20:55.233184 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.233331 SQW02       done calling COM method
07:20:55.233337 SQW02       convert COM output "accessible" back to caller format
07:20:55.233338 SQW02       done converting COM output "accessible" back to caller format
07:20:55.233340 main     Pumping COM event queue
07:20:55.233357 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.234196 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.234221 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1965A (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.234225 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1965B (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.234359 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1965C (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.234366 SQW02    session destroyed, 0 sessions left open
07:20:55.234369 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.235788 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.235965 main     Pumping COM event queue
07:20:55.235982 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220cdc0 (IUnknown*=0x0000010220cdc0; COM refcount now 3/4), new ID is 1965D; now 1 objects total
07:20:55.235991 SQW02       * authenticate: created session object with comptr 0x0000010220cdc0, MOR = 1d808dfc52259ff6-000000000001965d
07:20:55.236058 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1965E; now 2 objects total
07:20:55.236062 SQW02    VirtualBox object ref is 1d808dfc52259ff6-000000000001965e
07:20:55.236065 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.236586 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.236593 SQW02       findRefFromId(): looking up objref 1d808dfc52259ff6-000000000001965e
07:20:55.236597 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.236601 SQW02       calling COM method FindMachine
07:20:55.236834 main     Pumping COM event queue
07:20:55.236840 SQW02       done calling COM method
07:20:55.236844 SQW02       convert COM output "returnval" back to caller format
07:20:55.236976 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100850 (IUnknown*=0x00000106300370; COM refcount now 3/2), new ID is 1965F; now 3 objects total
07:20:55.236985 SQW02       done converting COM output "returnval" back to caller format
07:20:55.236990 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.236995 main     Pumping COM event queue
07:20:55.237479 SQW02    -- entering __vbox__IMachine_USCOREgetState
07:20:55.237486 SQW02       findRefFromId(): looking up objref 1d808dfc52259ff6-000000000001965f
07:20:55.237492 SQW02       findComPtrFromId(): returning original IMachine*=0x106100850 (IUnknown*=0x106300370)
07:20:55.237496 SQW02       calling COM method COMGETTER(State)
07:20:55.237728 SQW02       done calling COM method
07:20:55.237740 SQW02       convert COM output "state" back to caller format
07:20:55.237743 SQW02       done converting COM output "state" back to caller format
07:20:55.237749 main     Pumping COM event queue
07:20:55.237762 SQW02    -- leaving __vbox__IMachine_USCOREgetState, rc: 0x0 (0)
07:20:55.240433 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.240486 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1965D (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.240494 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1965E (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.240694 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1965F (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.240708 SQW02    session destroyed, 0 sessions left open
07:20:55.240713 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.249719 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.249893 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220efd0 (IUnknown*=0x0000010220efd0; COM refcount now 3/4), new ID is 19660; now 1 objects total
07:20:55.249906 main     Pumping COM event queue
07:20:55.249916 SQW02       * authenticate: created session object with comptr 0x0000010220efd0, MOR = c4fcd90b02d9400a-0000000000019660
07:20:55.250029 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19661; now 2 objects total
07:20:55.250035 SQW02    VirtualBox object ref is c4fcd90b02d9400a-0000000000019661
07:20:55.250039 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.250624 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.250631 SQW02       findRefFromId(): looking up objref c4fcd90b02d9400a-0000000000019661
07:20:55.250636 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.250640 SQW02       calling COM method FindMachine
07:20:55.250807 SQW02       done calling COM method
07:20:55.250817 SQW02       convert COM output "returnval" back to caller format
07:20:55.250825 main     Pumping COM event queue
07:20:55.250983 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106200120 (IUnknown*=0x00000106200000; COM refcount now 3/2), new ID is 19662; now 3 objects total
07:20:55.250998 main     Pumping COM event queue
07:20:55.251006 SQW02       done converting COM output "returnval" back to caller format
07:20:55.251018 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.251530 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.251537 SQW02       findRefFromId(): looking up objref c4fcd90b02d9400a-0000000000019662
07:20:55.251541 SQW02       findComPtrFromId(): returning original IMachine*=0x106200120 (IUnknown*=0x106200000)
07:20:55.251545 SQW02       calling COM method GetExtraData
07:20:55.251706 SQW02       done calling COM method
07:20:55.251718 main     Pumping COM event queue
07:20:55.251728 SQW02       convert COM output "returnval" back to caller format
07:20:55.251738 SQW02       done converting COM output "returnval" back to caller format
07:20:55.251742 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.252610 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.252625 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19660 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.252630 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19661 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.252771 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19662 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.252780 SQW02    session destroyed, 0 sessions left open
07:20:55.252782 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.260347 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.260657 main     Pumping COM event queue
07:20:55.260668 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106100f50 (IUnknown*=0x00000106100f50; COM refcount now 3/4), new ID is 19663; now 1 objects total
07:20:55.260677 SQW02       * authenticate: created session object with comptr 0x00000106100f50, MOR = 4c4e45b11dc00872-0000000000019663
07:20:55.260805 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19664; now 2 objects total
07:20:55.260817 SQW02    VirtualBox object ref is 4c4e45b11dc00872-0000000000019664
07:20:55.260821 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.261661 SQW02    -- entering __vbox__IVirtualBox_USCOREgetMachinesByGroups
07:20:55.261670 SQW02       findRefFromId(): looking up objref 4c4e45b11dc00872-0000000000019664
07:20:55.261676 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.261682 SQW02       calling COM method GetMachinesByGroups
07:20:55.261905 SQW02       done calling COM method
07:20:55.261924 main     Pumping COM event queue
07:20:55.261936 SQW02       convert COM output "returnval" back to caller format
07:20:55.262200 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100390 (IUnknown*=0x00000102500850; COM refcount now 3/3), new ID is 19665; now 3 objects total
07:20:55.262232 main     Pumping COM event queue
07:20:55.262270 SQW02       done converting COM output "returnval" back to caller format
07:20:55.262286 SQW02    -- leaving __vbox__IVirtualBox_USCOREgetMachinesByGroups, rc: 0x0 (0)
07:20:55.262969 SQW02    -- entering __vbox__IMachine_USCOREgetId
07:20:55.262978 SQW02       findRefFromId(): looking up objref 4c4e45b11dc00872-0000000000019665
07:20:55.262983 SQW02       findComPtrFromId(): returning original IMachine*=0x106100390 (IUnknown*=0x102500850)
07:20:55.262987 SQW02       calling COM method COMGETTER(Id)
07:20:55.263182 SQW02       done calling COM method
07:20:55.263192 main     Pumping COM event queue
07:20:55.263214 SQW02       convert COM output "id" back to caller format
07:20:55.263224 SQW02       done converting COM output "id" back to caller format
07:20:55.263228 SQW02    -- leaving __vbox__IMachine_USCOREgetId, rc: 0x0 (0)
07:20:55.264193 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.264210 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19663 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.264216 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19664 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.264364 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19665 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.264374 SQW02    session destroyed, 0 sessions left open
07:20:55.264377 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.266470 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.266649 main     Pumping COM event queue
07:20:55.266671 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106300070 (IUnknown*=0x00000106300070; COM refcount now 3/4), new ID is 19666; now 1 objects total
07:20:55.266681 SQW02       * authenticate: created session object with comptr 0x00000106300070, MOR = 47b0a014d60dd822-0000000000019666
07:20:55.266770 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19667; now 2 objects total
07:20:55.266776 SQW02    VirtualBox object ref is 47b0a014d60dd822-0000000000019667
07:20:55.266779 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.267334 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.267341 SQW02       findRefFromId(): looking up objref 47b0a014d60dd822-0000000000019667
07:20:55.267346 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.267350 SQW02       calling COM method FindMachine
07:20:55.267518 SQW02       done calling COM method
07:20:55.267555 main     Pumping COM event queue
07:20:55.267567 SQW02       convert COM output "returnval" back to caller format
07:20:55.267776 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106300180 (IUnknown*=0x00000102309130; COM refcount now 3/2), new ID is 19668; now 3 objects total
07:20:55.267787 main     Pumping COM event queue
07:20:55.267799 SQW02       done converting COM output "returnval" back to caller format
07:20:55.267810 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.268308 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.268314 SQW02       findRefFromId(): looking up objref 47b0a014d60dd822-0000000000019668
07:20:55.268318 SQW02       findComPtrFromId(): returning original IMachine*=0x106300180 (IUnknown*=0x102309130)
07:20:55.268321 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.268451 SQW02       done calling COM method
07:20:55.268463 main     Pumping COM event queue
07:20:55.268470 SQW02       convert COM output "accessible" back to caller format
07:20:55.268475 SQW02       done converting COM output "accessible" back to caller format
07:20:55.268478 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.269279 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.269293 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19666 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.269297 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19667 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.269412 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19668 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.269422 SQW02    session destroyed, 0 sessions left open
07:20:55.269426 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.270822 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.270976 main     Pumping COM event queue
07:20:55.270989 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f350 (IUnknown*=0x0000010220f350; COM refcount now 3/4), new ID is 19669; now 1 objects total
07:20:55.270999 SQW02       * authenticate: created session object with comptr 0x0000010220f350, MOR = a2d3ec1b31dbe403-0000000000019669
07:20:55.271076 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1966A; now 2 objects total
07:20:55.271083 SQW02    VirtualBox object ref is a2d3ec1b31dbe403-000000000001966a
07:20:55.271086 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.271599 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.271605 SQW02       findRefFromId(): looking up objref a2d3ec1b31dbe403-000000000001966a
07:20:55.271609 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.271613 SQW02       calling COM method FindMachine
07:20:55.271758 SQW02       done calling COM method
07:20:55.271765 SQW02       convert COM output "returnval" back to caller format
07:20:55.271773 main     Pumping COM event queue
07:20:55.271903 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106300020 (IUnknown*=0x00000102600ee0; COM refcount now 3/2), new ID is 1966B; now 3 objects total
07:20:55.271915 main     Pumping COM event queue
07:20:55.271921 SQW02       done converting COM output "returnval" back to caller format
07:20:55.271930 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.272448 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.272456 SQW02       findRefFromId(): looking up objref a2d3ec1b31dbe403-000000000001966b
07:20:55.272460 SQW02       findComPtrFromId(): returning original IMachine*=0x106300020 (IUnknown*=0x102600EE0)
07:20:55.272463 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.272600 SQW02       done calling COM method
07:20:55.272605 SQW02       convert COM output "accessible" back to caller format
07:20:55.272610 SQW02       done converting COM output "accessible" back to caller format
07:20:55.272621 main     Pumping COM event queue
07:20:55.272627 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.273481 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.273497 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19669 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.273502 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1966A (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.273644 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1966B (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.273651 SQW02    session destroyed, 0 sessions left open
07:20:55.273654 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.275048 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.275202 main     Pumping COM event queue
07:20:55.275208 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f000 (IUnknown*=0x0000010220f000; COM refcount now 3/4), new ID is 1966C; now 1 objects total
07:20:55.275214 SQW02       * authenticate: created session object with comptr 0x0000010220f000, MOR = 355674cb484cb340-000000000001966c
07:20:55.275285 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1966D; now 2 objects total
07:20:55.275291 SQW02    VirtualBox object ref is 355674cb484cb340-000000000001966d
07:20:55.275294 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.275794 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.275802 SQW02       findRefFromId(): looking up objref 355674cb484cb340-000000000001966d
07:20:55.275806 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.275810 SQW02       calling COM method FindMachine
07:20:55.275950 SQW02       done calling COM method
07:20:55.275959 main     Pumping COM event queue
07:20:55.275966 SQW02       convert COM output "returnval" back to caller format
07:20:55.276081 main     Pumping COM event queue
07:20:55.276088 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100390 (IUnknown*=0x00000102700430; COM refcount now 3/2), new ID is 1966E; now 3 objects total
07:20:55.276094 SQW02       done converting COM output "returnval" back to caller format
07:20:55.276097 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.276567 SQW02    -- entering __vbox__IMachine_USCOREgetState
07:20:55.276574 SQW02       findRefFromId(): looking up objref 355674cb484cb340-000000000001966e
07:20:55.276579 SQW02       findComPtrFromId(): returning original IMachine*=0x106100390 (IUnknown*=0x102700430)
07:20:55.276582 SQW02       calling COM method COMGETTER(State)
07:20:55.276708 SQW02       done calling COM method
07:20:55.276714 SQW02       convert COM output "state" back to caller format
07:20:55.276718 SQW02       done converting COM output "state" back to caller format
07:20:55.276721 SQW02    -- leaving __vbox__IMachine_USCOREgetState, rc: 0x0 (0)
07:20:55.276726 main     Pumping COM event queue
07:20:55.277530 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.277543 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1966C (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.277547 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1966D (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.277655 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1966E (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.277661 SQW02    session destroyed, 0 sessions left open
07:20:55.277663 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.281677 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.281877 main     Pumping COM event queue
07:20:55.281888 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000102309780 (IUnknown*=0x00000102309780; COM refcount now 3/4), new ID is 1966F; now 1 objects total
07:20:55.281896 SQW02       * authenticate: created session object with comptr 0x00000102309780, MOR = 7fd3730f56738e2c-000000000001966f
07:20:55.281982 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19670; now 2 objects total
07:20:55.281989 SQW02    VirtualBox object ref is 7fd3730f56738e2c-0000000000019670
07:20:55.281992 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.282573 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.282580 SQW02       findRefFromId(): looking up objref 7fd3730f56738e2c-0000000000019670
07:20:55.282584 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.282588 SQW02       calling COM method FindMachine
07:20:55.282752 SQW02       done calling COM method
07:20:55.282759 SQW02       convert COM output "returnval" back to caller format
07:20:55.282762 main     Pumping COM event queue
07:20:55.282931 main     Pumping COM event queue
07:20:55.282943 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102601020 (IUnknown*=0x00000102309190; COM refcount now 3/2), new ID is 19671; now 3 objects total
07:20:55.282949 SQW02       done converting COM output "returnval" back to caller format
07:20:55.282952 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.283454 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.283459 SQW02       findRefFromId(): looking up objref 7fd3730f56738e2c-0000000000019671
07:20:55.283463 SQW02       findComPtrFromId(): returning original IMachine*=0x102601020 (IUnknown*=0x102309190)
07:20:55.283467 SQW02       calling COM method GetExtraData
07:20:55.283629 SQW02       done calling COM method
07:20:55.283636 SQW02       convert COM output "returnval" back to caller format
07:20:55.283638 main     Pumping COM event queue
07:20:55.283650 SQW02       done converting COM output "returnval" back to caller format
07:20:55.283654 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.284429 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.284444 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1966F (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.284448 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19670 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.284592 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19671 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.284600 SQW02    session destroyed, 0 sessions left open
07:20:55.284603 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.288636 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.288821 main     Pumping COM event queue
07:20:55.288832 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f340 (IUnknown*=0x0000010220f340; COM refcount now 3/4), new ID is 19672; now 1 objects total
07:20:55.288840 SQW02       * authenticate: created session object with comptr 0x0000010220f340, MOR = f8dd3f62f285c2a0-0000000000019672
07:20:55.288929 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19673; now 2 objects total
07:20:55.288937 SQW02    VirtualBox object ref is f8dd3f62f285c2a0-0000000000019673
07:20:55.288940 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.289500 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.289506 SQW02       findRefFromId(): looking up objref f8dd3f62f285c2a0-0000000000019673
07:20:55.289510 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.289514 SQW02       calling COM method FindMachine
07:20:55.289671 SQW02       done calling COM method
07:20:55.289683 main     Pumping COM event queue
07:20:55.289694 SQW02       convert COM output "returnval" back to caller format
07:20:55.289856 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102601020 (IUnknown*=0x00000102600f10; COM refcount now 3/2), new ID is 19674; now 3 objects total
07:20:55.289867 main     Pumping COM event queue
07:20:55.289876 SQW02       done converting COM output "returnval" back to caller format
07:20:55.289885 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.290390 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.290397 SQW02       findRefFromId(): looking up objref f8dd3f62f285c2a0-0000000000019674
07:20:55.290401 SQW02       findComPtrFromId(): returning original IMachine*=0x102601020 (IUnknown*=0x102600F10)
07:20:55.290405 SQW02       calling COM method GetExtraData
07:20:55.290560 SQW02       done calling COM method
07:20:55.290568 SQW02       convert COM output "returnval" back to caller format
07:20:55.290571 main     Pumping COM event queue
07:20:55.290586 SQW02       done converting COM output "returnval" back to caller format
07:20:55.290592 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.291391 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.291405 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19672 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.291409 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19673 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.291527 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19674 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.291536 SQW02    session destroyed, 0 sessions left open
07:20:55.291539 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.292917 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.293060 main     Pumping COM event queue
07:20:55.293068 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000102600f30 (IUnknown*=0x00000102600f30; COM refcount now 3/4), new ID is 19675; now 1 objects total
07:20:55.293076 SQW02       * authenticate: created session object with comptr 0x00000102600f30, MOR = f37b9b133938fa49-0000000000019675
07:20:55.293127 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19676; now 2 objects total
07:20:55.293133 SQW02    VirtualBox object ref is f37b9b133938fa49-0000000000019676
07:20:55.293136 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.293628 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.293633 SQW02       findRefFromId(): looking up objref f37b9b133938fa49-0000000000019676
07:20:55.293637 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.293640 SQW02       calling COM method FindMachine
07:20:55.293791 SQW02       done calling COM method
07:20:55.293799 SQW02       convert COM output "returnval" back to caller format
07:20:55.293803 main     Pumping COM event queue
07:20:55.293958 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100390 (IUnknown*=0x00000102700430; COM refcount now 3/2), new ID is 19677; now 3 objects total
07:20:55.293964 main     Pumping COM event queue
07:20:55.293975 SQW02       done converting COM output "returnval" back to caller format
07:20:55.293981 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.294453 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.294460 SQW02       findRefFromId(): looking up objref f37b9b133938fa49-0000000000019677
07:20:55.294464 SQW02       findComPtrFromId(): returning original IMachine*=0x106100390 (IUnknown*=0x102700430)
07:20:55.294467 SQW02       calling COM method GetExtraData
07:20:55.294599 SQW02       done calling COM method
07:20:55.294606 SQW02       convert COM output "returnval" back to caller format
07:20:55.294610 main     Pumping COM event queue
07:20:55.294625 SQW02       done converting COM output "returnval" back to caller format
07:20:55.294631 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.295375 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.295390 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19675 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.295394 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19676 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.295507 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19677 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.295513 SQW02    session destroyed, 0 sessions left open
07:20:55.295515 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.296923 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.297060 main     Pumping COM event queue
07:20:55.297074 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000102601070 (IUnknown*=0x00000102601070; COM refcount now 3/4), new ID is 19678; now 1 objects total
07:20:55.297082 SQW02       * authenticate: created session object with comptr 0x00000102601070, MOR = 5b7773e33bf27423-0000000000019678
07:20:55.297139 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19679; now 2 objects total
07:20:55.297143 SQW02    VirtualBox object ref is 5b7773e33bf27423-0000000000019679
07:20:55.297146 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.297626 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.297631 SQW02       findRefFromId(): looking up objref 5b7773e33bf27423-0000000000019679
07:20:55.297635 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.297638 SQW02       calling COM method FindMachine
07:20:55.297771 SQW02       done calling COM method
07:20:55.297783 SQW02       convert COM output "returnval" back to caller format
07:20:55.297789 main     Pumping COM event queue
07:20:55.297925 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106300370 (IUnknown*=0x00000102605850; COM refcount now 3/2), new ID is 1967A; now 3 objects total
07:20:55.297933 SQW02       done converting COM output "returnval" back to caller format
07:20:55.297938 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.297944 main     Pumping COM event queue
07:20:55.298423 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.298428 SQW02       findRefFromId(): looking up objref 5b7773e33bf27423-000000000001967a
07:20:55.298432 SQW02       findComPtrFromId(): returning original IMachine*=0x106300370 (IUnknown*=0x102605850)
07:20:55.298434 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.298557 SQW02       done calling COM method
07:20:55.298563 SQW02       convert COM output "accessible" back to caller format
07:20:55.298565 SQW02       done converting COM output "accessible" back to caller format
07:20:55.298568 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.298571 main     Pumping COM event queue
07:20:55.299416 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.299431 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19678 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.299436 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19679 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.299594 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1967A (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.299603 SQW02    session destroyed, 0 sessions left open
07:20:55.299606 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.300956 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.301133 main     Pumping COM event queue
07:20:55.301145 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f6a0 (IUnknown*=0x0000010220f6a0; COM refcount now 3/4), new ID is 1967B; now 1 objects total
07:20:55.301154 SQW02       * authenticate: created session object with comptr 0x0000010220f6a0, MOR = a160ef05de0af4c2-000000000001967b
07:20:55.301247 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1967C; now 2 objects total
07:20:55.301252 SQW02    VirtualBox object ref is a160ef05de0af4c2-000000000001967c
07:20:55.301257 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.302072 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.302079 SQW02       findRefFromId(): looking up objref a160ef05de0af4c2-000000000001967c
07:20:55.302084 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.302091 SQW02       calling COM method FindMachine
07:20:55.302277 SQW02       done calling COM method
07:20:55.302292 SQW02       convert COM output "returnval" back to caller format
07:20:55.302300 main     Pumping COM event queue
07:20:55.302438 main     Pumping COM event queue
07:20:55.302449 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102500850 (IUnknown*=0x0000010230a2e0; COM refcount now 3/2), new ID is 1967D; now 3 objects total
07:20:55.302456 SQW02       done converting COM output "returnval" back to caller format
07:20:55.302460 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.303076 SQW02    -- entering __vbox__IMachine_USCOREgetState
07:20:55.303088 SQW02       findRefFromId(): looking up objref a160ef05de0af4c2-000000000001967d
07:20:55.303097 SQW02       findComPtrFromId(): returning original IMachine*=0x102500850 (IUnknown*=0x10230A2E0)
07:20:55.303102 SQW02       calling COM method COMGETTER(State)
07:20:55.303369 SQW02       done calling COM method
07:20:55.303378 SQW02       convert COM output "state" back to caller format
07:20:55.303383 SQW02       done converting COM output "state" back to caller format
07:20:55.303388 SQW02    -- leaving __vbox__IMachine_USCOREgetState, rc: 0x0 (0)
07:20:55.303402 main     Pumping COM event queue
07:20:55.305213 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.305243 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1967B (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.305250 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1967C (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.305437 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1967D (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.305447 SQW02    session destroyed, 0 sessions left open
07:20:55.305450 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.307237 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.307448 main     Pumping COM event queue
07:20:55.307455 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f9b0 (IUnknown*=0x0000010220f9b0; COM refcount now 3/4), new ID is 1967E; now 1 objects total
07:20:55.307473 SQW02       * authenticate: created session object with comptr 0x0000010220f9b0, MOR = 620b4c9d8fce5408-000000000001967e
07:20:55.307565 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1967F; now 2 objects total
07:20:55.307573 SQW02    VirtualBox object ref is 620b4c9d8fce5408-000000000001967f
07:20:55.307578 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.308219 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.308226 SQW02       findRefFromId(): looking up objref 620b4c9d8fce5408-000000000001967f
07:20:55.308231 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.308235 SQW02       calling COM method FindMachine
07:20:55.308406 SQW02       done calling COM method
07:20:55.308411 SQW02       convert COM output "returnval" back to caller format
07:20:55.308417 main     Pumping COM event queue
07:20:55.308550 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x000001061019f0 (IUnknown*=0x00000106101b10; COM refcount now 3/2), new ID is 19680; now 3 objects total
07:20:55.308570 main     Pumping COM event queue
07:20:55.308581 SQW02       done converting COM output "returnval" back to caller format
07:20:55.308610 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.309214 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.309221 SQW02       findRefFromId(): looking up objref 620b4c9d8fce5408-0000000000019680
07:20:55.309227 SQW02       findComPtrFromId(): returning original IMachine*=0x1061019F0 (IUnknown*=0x106101B10)
07:20:55.309231 SQW02       calling COM method GetExtraData
07:20:55.309412 SQW02       done calling COM method
07:20:55.309424 SQW02       convert COM output "returnval" back to caller format
07:20:55.309431 main     Pumping COM event queue
07:20:55.309443 SQW02       done converting COM output "returnval" back to caller format
07:20:55.309454 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.310368 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.310389 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1967E (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.310396 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1967F (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.310539 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19680 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.310550 SQW02    session destroyed, 0 sessions left open
07:20:55.310555 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.312026 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.312191 main     Pumping COM event queue
07:20:55.312203 SQW02       * ManagedObjectRef: MOR created for ISession*=0x000001061016d0 (IUnknown*=0x000001061016d0; COM refcount now 3/4), new ID is 19681; now 1 objects total
07:20:55.312210 SQW02       * authenticate: created session object with comptr 0x000001061016d0, MOR = 89c4a27cbade72e5-0000000000019681
07:20:55.312289 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19682; now 2 objects total
07:20:55.312296 SQW02    VirtualBox object ref is 89c4a27cbade72e5-0000000000019682
07:20:55.312301 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.312819 SQW02    -- entering __vbox__IWebsessionManager_USCOREgetSessionObject
07:20:55.312827 SQW02    -- leaving __vbox__IWebsessionManager_USCOREgetSessionObject, rc: 0x0
07:20:55.313285 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.313292 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019682
07:20:55.313296 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.313300 SQW02       calling COM method FindMachine
07:20:55.313442 SQW02       done calling COM method
07:20:55.313451 SQW02       convert COM output "returnval" back to caller format
07:20:55.313458 main     Pumping COM event queue
07:20:55.313604 main     Pumping COM event queue
07:20:55.313616 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x000001061020b0 (IUnknown*=0x00000106200000; COM refcount now 3/2), new ID is 19683; now 3 objects total
07:20:55.313624 SQW02       done converting COM output "returnval" back to caller format
07:20:55.313630 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.314142 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.314148 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019683
07:20:55.314152 SQW02       findComPtrFromId(): returning original IMachine*=0x1061020B0 (IUnknown*=0x106200000)
07:20:55.314155 SQW02       calling COM method COMGETTER(Name)
07:20:55.314299 SQW02       done calling COM method
07:20:55.314311 main     Pumping COM event queue
07:20:55.314317 SQW02       convert COM output "name" back to caller format
07:20:55.314325 SQW02       done converting COM output "name" back to caller format
07:20:55.314331 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.315003 SQW02    -- entering __vbox__IMachine_USCORElockMachine
07:20:55.315009 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019683
07:20:55.315014 SQW02       findComPtrFromId(): returning original IMachine*=0x1061020B0 (IUnknown*=0x106200000)
07:20:55.315017 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019681
07:20:55.315020 SQW02       findComPtrFromId(): returning original ISession*=0x1061016D0 (IUnknown*=0x1061016D0)
07:20:55.315022 SQW02       calling COM method LockMachine
07:20:55.315167 main     Pumping COM event queue
07:20:55.315283 main     Pumping COM event queue
07:20:55.315512 main     Pumping COM event queue
07:20:55.315623 main     Pumping COM event queue
07:20:55.315740 main     Pumping COM event queue
07:20:55.315845 SQW02       done calling COM method
07:20:55.315854 SQW02    -- leaving __vbox__IMachine_USCORElockMachine, rc: 0x0 (0)
07:20:55.315862 main     Pumping COM event queue
07:20:55.315938 main     Pumping COM event queue
07:20:55.316369 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.316375 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019681
07:20:55.316379 SQW02       findComPtrFromId(): returning original ISession*=0x1061016D0 (IUnknown*=0x1061016D0)
07:20:55.316382 SQW02       calling COM method COMGETTER(Machine)
07:20:55.316506 SQW02       done calling COM method
07:20:55.316514 SQW02       convert COM output "machine" back to caller format
07:20:55.316521 main     Pumping COM event queue
07:20:55.316638 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102605040 (IUnknown*=0x00000106101920; COM refcount now 3/3), new ID is 19684; now 4 objects total
07:20:55.316647 SQW02       done converting COM output "machine" back to caller format
07:20:55.316652 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.316657 main     Pumping COM event queue
07:20:55.317145 SQW02    -- entering __vbox__ISession_USCOREgetConsole
07:20:55.317151 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019681
07:20:55.317155 SQW02       findComPtrFromId(): returning original ISession*=0x1061016D0 (IUnknown*=0x1061016D0)
07:20:55.317158 SQW02       calling COM method COMGETTER(Console)
07:20:55.317421 SQW02       done calling COM method
07:20:55.317427 main     Pumping COM event queue
07:20:55.317437 SQW02       convert COM output "console" back to caller format
07:20:55.317560 SQW02       * ManagedObjectRef: MOR created for IConsole*=0x000001026054e0 (IUnknown*=0x00000102605570; COM refcount now 3/3), new ID is 19685; now 5 objects total
07:20:55.317568 SQW02       done converting COM output "console" back to caller format
07:20:55.317573 SQW02    -- leaving __vbox__ISession_USCOREgetConsole, rc: 0x0 (0)
07:20:55.317578 main     Pumping COM event queue
07:20:55.318084 SQW02    -- entering __vbox__IConsole_USCOREgetMachine
07:20:55.318091 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019685
07:20:55.318095 SQW02       findComPtrFromId(): returning original IConsole*=0x1026054E0 (IUnknown*=0x102605570)
07:20:55.318098 SQW02       calling COM method COMGETTER(Machine)
07:20:55.318405 SQW02       done calling COM method
07:20:55.318414 SQW02       convert COM output "machine" back to caller format
07:20:55.318419 main     Pumping COM event queue
07:20:55.318428 SQW02       findRefFromPtr: found existing ref 89c4a27cbade72e5-0000000000019684 (IMachine) for COM obj 0x106101920
07:20:55.318435 SQW02       done converting COM output "machine" back to caller format
07:20:55.318491 SQW02    -- leaving __vbox__IConsole_USCOREgetMachine, rc: 0x0 (0)
07:20:55.318991 SQW02    -- entering __vbox__IMachine_USCOREgetGuestPropertyValue
07:20:55.318998 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019684
07:20:55.319002 SQW02       findComPtrFromId(): returning original IMachine*=0x102605040 (IUnknown*=0x106101920)
07:20:55.319006 SQW02       calling COM method GetGuestPropertyValue
07:20:55.319301 SQW02       done calling COM method
07:20:55.319311 SQW02       convert COM output "returnval" back to caller format
07:20:55.319317 main     Pumping COM event queue
07:20:55.319333 SQW02       done converting COM output "returnval" back to caller format
07:20:55.319339 SQW02    -- leaving __vbox__IMachine_USCOREgetGuestPropertyValue, rc: 0x0 (0)
07:20:55.319885 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.319894 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019681
07:20:55.319899 SQW02       findComPtrFromId(): returning original ISession*=0x1061016D0 (IUnknown*=0x1061016D0)
07:20:55.319902 SQW02       calling COM method COMGETTER(Machine)
07:20:55.320056 SQW02       done calling COM method
07:20:55.320068 SQW02       convert COM output "machine" back to caller format
07:20:55.320074 main     Pumping COM event queue
07:20:55.320096 SQW02       findRefFromPtr: found existing ref 89c4a27cbade72e5-0000000000019684 (IMachine) for COM obj 0x106101920
07:20:55.320108 SQW02       done converting COM output "machine" back to caller format
07:20:55.320198 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.320826 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.320835 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019684
07:20:55.320841 SQW02       findComPtrFromId(): returning original IMachine*=0x102605040 (IUnknown*=0x106101920)
07:20:55.320846 SQW02       calling COM method COMGETTER(Name)
07:20:55.320987 SQW02       done calling COM method
07:20:55.320999 SQW02       convert COM output "name" back to caller format
07:20:55.321005 main     Pumping COM event queue
07:20:55.321024 SQW02       done converting COM output "name" back to caller format
07:20:55.321035 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.321765 SQW02    -- entering __vbox__ISession_USCOREunlockMachine
07:20:55.321771 SQW02       findRefFromId(): looking up objref 89c4a27cbade72e5-0000000000019681
07:20:55.321775 SQW02       findComPtrFromId(): returning original ISession*=0x1061016D0 (IUnknown*=0x1061016D0)
07:20:55.321778 SQW02       calling COM method UnlockMachine
07:20:55.321993 main     Pumping COM event queue
07:20:55.322117 main     Pumping COM event queue
07:20:55.322164 main     Pumping COM event queue
07:20:55.322213 main     Pumping COM event queue
07:20:55.322289 main     Pumping COM event queue
07:20:55.322331 SQW02       done calling COM method
07:20:55.322339 SQW02    -- leaving __vbox__ISession_USCOREunlockMachine, rc: 0x0 (0)
07:20:55.323261 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.323279 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19681 (ISession; COM refcount now 0/1); now 4 objects total
07:20:55.323286 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19682 (IVirtualBox; COM refcount now 1/2); now 3 objects total
07:20:55.323395 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19683 (IMachine; COM refcount now 0/0); now 2 objects total
07:20:55.323506 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19684 (IMachine; COM refcount now 0/0); now 1 objects total
07:20:55.323627 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19685 (IConsole; COM refcount now 0/0); now 0 objects total
07:20:55.323638 SQW02    session destroyed, 0 sessions left open
07:20:55.323643 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.325090 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.325234 main     Pumping COM event queue
07:20:55.325246 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106300910 (IUnknown*=0x00000106300910; COM refcount now 3/4), new ID is 19686; now 1 objects total
07:20:55.325253 SQW02       * authenticate: created session object with comptr 0x00000106300910, MOR = dc8b82da31bb6c05-0000000000019686
07:20:55.325321 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 19687; now 2 objects total
07:20:55.325329 SQW02    VirtualBox object ref is dc8b82da31bb6c05-0000000000019687
07:20:55.325333 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.325866 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.325872 SQW02       findRefFromId(): looking up objref dc8b82da31bb6c05-0000000000019687
07:20:55.325876 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.325881 SQW02       calling COM method FindMachine
07:20:55.326024 SQW02       done calling COM method
07:20:55.326033 SQW02       convert COM output "returnval" back to caller format
07:20:55.326037 main     Pumping COM event queue
07:20:55.326171 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x000001023095b0 (IUnknown*=0x00000102210370; COM refcount now 3/2), new ID is 19688; now 3 objects total
07:20:55.326180 SQW02       done converting COM output "returnval" back to caller format
07:20:55.326186 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.326190 main     Pumping COM event queue
07:20:55.326660 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.326668 SQW02       findRefFromId(): looking up objref dc8b82da31bb6c05-0000000000019688
07:20:55.326672 SQW02       findComPtrFromId(): returning original IMachine*=0x1023095B0 (IUnknown*=0x102210370)
07:20:55.326674 SQW02       calling COM method COMGETTER(Name)
07:20:55.326796 SQW02       done calling COM method
07:20:55.326802 SQW02       convert COM output "name" back to caller format
07:20:55.326804 main     Pumping COM event queue
07:20:55.326813 SQW02       done converting COM output "name" back to caller format
07:20:55.326817 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.327648 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.327663 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19686 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.327667 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19687 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.327793 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19688 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.327801 SQW02    session destroyed, 0 sessions left open
07:20:55.327805 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.329223 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.329377 main     Pumping COM event queue
07:20:55.329397 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f460 (IUnknown*=0x0000010220f460; COM refcount now 3/4), new ID is 19689; now 1 objects total
07:20:55.329405 SQW02       * authenticate: created session object with comptr 0x0000010220f460, MOR = b2fcce4ee3aacdb2-0000000000019689
07:20:55.329475 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1968A; now 2 objects total
07:20:55.329482 SQW02    VirtualBox object ref is b2fcce4ee3aacdb2-000000000001968a
07:20:55.329484 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.330021 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.330026 SQW02       findRefFromId(): looking up objref b2fcce4ee3aacdb2-000000000001968a
07:20:55.330030 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.330034 SQW02       calling COM method FindMachine
07:20:55.330191 SQW02       done calling COM method
07:20:55.330196 SQW02       convert COM output "returnval" back to caller format
07:20:55.330200 main     Pumping COM event queue
07:20:55.330323 main     Pumping COM event queue
07:20:55.330331 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x000001061021f0 (IUnknown*=0x00000102210220; COM refcount now 3/2), new ID is 1968B; now 3 objects total
07:20:55.330338 SQW02       done converting COM output "returnval" back to caller format
07:20:55.330341 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.330838 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.330845 SQW02       findRefFromId(): looking up objref b2fcce4ee3aacdb2-000000000001968b
07:20:55.330850 SQW02       findComPtrFromId(): returning original IMachine*=0x1061021F0 (IUnknown*=0x102210220)
07:20:55.330853 SQW02       calling COM method GetExtraData
07:20:55.330989 SQW02       done calling COM method
07:20:55.330995 SQW02       convert COM output "returnval" back to caller format
07:20:55.330999 SQW02       done converting COM output "returnval" back to caller format
07:20:55.331005 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.331011 main     Pumping COM event queue
07:20:55.331878 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.331894 SQW02       * ~ManagedObjectRef: deleting MOR for ID 19689 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.331899 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1968A (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.332014 SQW02       * ~ManagedObjectRef: deleting MOR for ID 1968B (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.332022 SQW02    session destroyed, 0 sessions left open
07:20:55.332025 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.333458 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.333593 main     Pumping COM event queue
07:20:55.333604 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010230ad10 (IUnknown*=0x0000010230ad10; COM refcount now 3/4), new ID is 1968C; now 1 objects total
07:20:55.333611 SQW02       * authenticate: created session object with comptr 0x0000010230ad10, MOR = 976d37b9f3f5d848-000000000001968c
07:20:55.333667 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 1968D; now 2 objects total
07:20:55.333674 SQW02    VirtualBox object ref is 976d37b9f3f5d848-000000000001968d
07:20:55.333678 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.334179 SQW02    -- entering __vbox__IWebsessionManager_USCOREgetSessionObject
07:20:55.334186 SQW02    -- leaving __vbox__IWebsessionManager_USCOREgetSessionObject, rc: 0x0
07:20:55.334621 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.334625 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968d
07:20:55.334629 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.334633 SQW02       calling COM method FindMachine
07:20:55.334765 SQW02       done calling COM method
07:20:55.334773 SQW02       convert COM output "returnval" back to caller format
07:20:55.334776 main     Pumping COM event queue
07:20:55.334891 main     Pumping COM event queue
07:20:55.334901 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102210060 (IUnknown*=0x0000010230a630; COM refcount now 3/2), new ID is 1968E; now 3 objects total
07:20:55.334907 SQW02       done converting COM output "returnval" back to caller format
07:20:55.334910 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.335381 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.335389 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968e
07:20:55.335393 SQW02       findComPtrFromId(): returning original IMachine*=0x102210060 (IUnknown*=0x10230A630)
07:20:55.335396 SQW02       calling COM method COMGETTER(Name)
07:20:55.335507 SQW02       done calling COM method
07:20:55.335512 SQW02       convert COM output "name" back to caller format
07:20:55.335515 main     Pumping COM event queue
07:20:55.335523 SQW02       done converting COM output "name" back to caller format
07:20:55.335527 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.336144 SQW02    -- entering __vbox__IMachine_USCORElockMachine
07:20:55.336149 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968e
07:20:55.336152 SQW02       findComPtrFromId(): returning original IMachine*=0x102210060 (IUnknown*=0x10230A630)
07:20:55.336155 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968c
07:20:55.336158 SQW02       findComPtrFromId(): returning original ISession*=0x10230AD10 (IUnknown*=0x10230AD10)
07:20:55.336160 SQW02       calling COM method LockMachine
07:20:55.336285 main     Pumping COM event queue
07:20:55.336385 main     Pumping COM event queue
07:20:55.336617 main     Pumping COM event queue
07:20:55.336716 main     Pumping COM event queue
07:20:55.336826 main     Pumping COM event queue
07:20:55.336919 SQW02       done calling COM method
07:20:55.336929 main     Pumping COM event queue
07:20:55.336934 SQW02    -- leaving __vbox__IMachine_USCORElockMachine, rc: 0x0 (0)
07:20:55.336957 main     Pumping COM event queue
07:20:55.337385 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.337391 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968c
07:20:55.337395 SQW02       findComPtrFromId(): returning original ISession*=0x10230AD10 (IUnknown*=0x10230AD10)
07:20:55.337398 SQW02       calling COM method COMGETTER(Machine)
07:20:55.337503 SQW02       done calling COM method
07:20:55.337511 SQW02       convert COM output "machine" back to caller format
07:20:55.337516 main     Pumping COM event queue
07:20:55.337618 main     Pumping COM event queue
07:20:55.337627 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102210220 (IUnknown*=0x0000010230aed0; COM refcount now 3/3), new ID is 1968F; now 4 objects total
07:20:55.337633 SQW02       done converting COM output "machine" back to caller format
07:20:55.337636 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.338092 SQW02    -- entering __vbox__IMachine_USCOREgetParent
07:20:55.338097 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.338101 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.338103 SQW02       calling COM method COMGETTER(Parent)
07:20:55.338205 SQW02       done calling COM method
07:20:55.338212 SQW02       convert COM output "parent" back to caller format
07:20:55.338223 main     Pumping COM event queue
07:20:55.338232 SQW02       findRefFromPtr: found existing ref 976d37b9f3f5d848-000000000001968d (IVirtualBox) for COM obj 0x1026038e0
07:20:55.338237 SQW02       done converting COM output "parent" back to caller format
07:20:55.338285 SQW02    -- leaving __vbox__IMachine_USCOREgetParent, rc: 0x0 (0)
07:20:55.338730 SQW02    -- entering __vbox__IVirtualBox_USCOREgetSystemProperties
07:20:55.338735 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968d
07:20:55.338739 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.338741 SQW02       calling COM method COMGETTER(SystemProperties)
07:20:55.338851 SQW02       done calling COM method
07:20:55.338855 SQW02       convert COM output "systemProperties" back to caller format
07:20:55.338860 main     Pumping COM event queue
07:20:55.338965 SQW02       * ManagedObjectRef: MOR created for ISystemProperties*=0x0000010230b0a0 (IUnknown*=0x00000106102120; COM refcount now 3/2), new ID is 19690; now 5 objects total
07:20:55.338976 main     Pumping COM event queue
07:20:55.338984 SQW02       done converting COM output "systemProperties" back to caller format
07:20:55.338988 SQW02    -- leaving __vbox__IVirtualBox_USCOREgetSystemProperties, rc: 0x0 (0)
07:20:55.339466 SQW02    -- entering __vbox__IMachine_USCOREgetChipsetType
07:20:55.339471 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.339475 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.339478 SQW02       calling COM method COMGETTER(ChipsetType)
07:20:55.339590 SQW02       done calling COM method
07:20:55.339596 main     Pumping COM event queue
07:20:55.339604 SQW02       convert COM output "chipsetType" back to caller format
07:20:55.339609 SQW02       done converting COM output "chipsetType" back to caller format
07:20:55.339612 SQW02    -- leaving __vbox__IMachine_USCOREgetChipsetType, rc: 0x0 (0)
07:20:55.340079 SQW02    -- entering __vbox__ISystemProperties_USCOREgetMaxNetworkAdapters
07:20:55.340084 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019690
07:20:55.340088 SQW02       findComPtrFromId(): returning original ISystemProperties*=0x10230B0A0 (IUnknown*=0x106102120)
07:20:55.340091 SQW02       calling COM method GetMaxNetworkAdapters
07:20:55.340193 SQW02       done calling COM method
07:20:55.340201 SQW02       convert COM output "returnval" back to caller format
07:20:55.340205 SQW02       done converting COM output "returnval" back to caller format
07:20:55.340210 SQW02    -- leaving __vbox__ISystemProperties_USCOREgetMaxNetworkAdapters, rc: 0x0 (0)
07:20:55.340216 main     Pumping COM event queue
07:20:55.340725 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.340730 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.340734 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.340737 SQW02       calling COM method GetNetworkAdapter
07:20:55.340849 SQW02       done calling COM method
07:20:55.340857 SQW02       convert COM output "returnval" back to caller format
07:20:55.340868 main     Pumping COM event queue
07:20:55.340983 main     Pumping COM event queue
07:20:55.340993 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x00000106101a00 (IUnknown*=0x00000106200000; COM refcount now 3/2), new ID is 19691; now 6 objects total
07:20:55.341000 SQW02       done converting COM output "returnval" back to caller format
07:20:55.341003 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.341473 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.341478 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.341482 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.341485 SQW02       calling COM method GetNetworkAdapter
07:20:55.341594 SQW02       done calling COM method
07:20:55.341602 SQW02       convert COM output "returnval" back to caller format
07:20:55.341608 main     Pumping COM event queue
07:20:55.341701 main     Pumping COM event queue
07:20:55.341710 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x000001027009d0 (IUnknown*=0x00000106101a90; COM refcount now 3/2), new ID is 19692; now 7 objects total
07:20:55.341717 SQW02       done converting COM output "returnval" back to caller format
07:20:55.341720 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.342185 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.342190 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.342194 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.342197 SQW02       calling COM method GetNetworkAdapter
07:20:55.342300 SQW02       done calling COM method
07:20:55.342305 SQW02       convert COM output "returnval" back to caller format
07:20:55.342308 main     Pumping COM event queue
07:20:55.342415 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x00000106200270 (IUnknown*=0x00000102700100; COM refcount now 3/2), new ID is 19693; now 8 objects total
07:20:55.342424 SQW02       done converting COM output "returnval" back to caller format
07:20:55.342428 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.342434 main     Pumping COM event queue
07:20:55.342891 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.342897 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.342901 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.342905 SQW02       calling COM method GetNetworkAdapter
07:20:55.343011 SQW02       done calling COM method
07:20:55.343019 SQW02       convert COM output "returnval" back to caller format
07:20:55.343025 main     Pumping COM event queue
07:20:55.343123 main     Pumping COM event queue
07:20:55.343133 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x00000106102580 (IUnknown*=0x00000106200300; COM refcount now 3/2), new ID is 19694; now 9 objects total
07:20:55.343140 SQW02       done converting COM output "returnval" back to caller format
07:20:55.343142 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.343608 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.343613 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.343617 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.343619 SQW02       calling COM method GetNetworkAdapter
07:20:55.343738 SQW02       done calling COM method
07:20:55.343746 SQW02       convert COM output "returnval" back to caller format
07:20:55.343751 main     Pumping COM event queue
07:20:55.343870 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x00000102605060 (IUnknown*=0x000001026057a0; COM refcount now 3/2), new ID is 19695; now 10 objects total
07:20:55.343878 SQW02       done converting COM output "returnval" back to caller format
07:20:55.343883 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.343887 main     Pumping COM event queue
07:20:55.344350 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.344355 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.344359 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.344362 SQW02       calling COM method GetNetworkAdapter
07:20:55.344471 SQW02       done calling COM method
07:20:55.344481 main     Pumping COM event queue
07:20:55.344489 SQW02       convert COM output "returnval" back to caller format
07:20:55.344603 main     Pumping COM event queue
07:20:55.344613 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x00000102700880 (IUnknown*=0x000001063008c0; COM refcount now 3/2), new ID is 19696; now 11 objects total
07:20:55.344621 SQW02       done converting COM output "returnval" back to caller format
07:20:55.344624 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.345089 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.345094 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.345098 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.345101 SQW02       calling COM method GetNetworkAdapter
07:20:55.345217 SQW02       done calling COM method
07:20:55.345228 SQW02       convert COM output "returnval" back to caller format
07:20:55.345236 main     Pumping COM event queue
07:20:55.345343 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x0000010220f410 (IUnknown*=0x00000106102cd0; COM refcount now 3/2), new ID is 19697; now 12 objects total
07:20:55.345352 SQW02       done converting COM output "returnval" back to caller format
07:20:55.345356 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.345361 main     Pumping COM event queue
07:20:55.345820 SQW02    -- entering __vbox__IMachine_USCOREgetNetworkAdapter
07:20:55.345825 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.345828 SQW02       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.345831 SQW02       calling COM method GetNetworkAdapter
07:20:55.345943 SQW02       done calling COM method
07:20:55.345955 main     Pumping COM event queue
07:20:55.345964 SQW02       convert COM output "returnval" back to caller format
07:20:55.346070 SQW02       * ManagedObjectRef: MOR created for INetworkAdapter*=0x000001062004f0 (IUnknown*=0x00000102605500; COM refcount now 3/2), new ID is 19698; now 13 objects total
07:20:55.346080 SQW02       done converting COM output "returnval" back to caller format
07:20:55.346087 main     Pumping COM event queue
07:20:55.346093 SQW02    -- leaving __vbox__IMachine_USCOREgetNetworkAdapter, rc: 0x0 (0)
07:20:55.346555 SQW02    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.346561 SQW02       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019691
07:20:55.346565 SQW02       findComPtrFromId(): returning original INetworkAdapter*=0x106101A00 (IUnknown*=0x106200000)
07:20:55.346568 SQW02       calling COM method COMGETTER(NATEngine)
07:20:55.346680 SQW02       done calling COM method
07:20:55.346687 main     Pumping COM event queue
07:20:55.346696 SQW02       convert COM output "NATEngine" back to caller format
07:20:55.346812 main     Pumping COM event queue
07:20:55.346820 SQW02       * ManagedObjectRef: MOR created for INATEngine*=0x00000106200580 (IUnknown*=0x000001022103d0; COM refcount now 3/2), new ID is 19699; now 14 objects total
07:20:55.346826 SQW02       done converting COM output "NATEngine" back to caller format
07:20:55.346829 SQW02    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.347287 SQPmp    Request 2336 on socket 8 queued for processing (1 items on Q)
07:20:55.347301 SQW01    Processing connection from IP=127.0.0.1 socket=8 (1 out of 2 threads idle)
07:20:55.347476 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.347482 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019699
07:20:55.347486 SQW01       findComPtrFromId(): returning original INATEngine*=0x106200580 (IUnknown*=0x1022103D0)
07:20:55.347489 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.347601 SQW01       done calling COM method
07:20:55.347606 SQW01       convert COM output "hostIP" back to caller format
07:20:55.347610 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.347616 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.347621 main     Pumping COM event queue
07:20:55.348098 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.348104 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019699
07:20:55.348108 SQW01       findComPtrFromId(): returning original INATEngine*=0x106200580 (IUnknown*=0x1022103D0)
07:20:55.348110 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.348221 SQW01       done calling COM method
07:20:55.348226 SQW01       convert COM output "redirects" back to caller format
07:20:55.348228 SQW01       done converting COM output "redirects" back to caller format
07:20:55.348236 main     Pumping COM event queue
07:20:55.348245 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.348693 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.348698 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019692
07:20:55.348702 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x1027009D0 (IUnknown*=0x106101A90)
07:20:55.348705 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.348816 SQW01       done calling COM method
07:20:55.348820 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.348826 main     Pumping COM event queue
07:20:55.348922 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x0000010220fb80 (IUnknown*=0x00000102600dd0; COM refcount now 3/2), new ID is 1969A; now 15 objects total
07:20:55.348932 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.348937 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.348941 main     Pumping COM event queue
07:20:55.349401 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.349407 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969a
07:20:55.349411 SQW01       findComPtrFromId(): returning original INATEngine*=0x10220FB80 (IUnknown*=0x102600DD0)
07:20:55.349413 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.349526 SQW01       done calling COM method
07:20:55.349533 SQW01       convert COM output "hostIP" back to caller format
07:20:55.349539 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.349545 main     Pumping COM event queue
07:20:55.349554 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.350002 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.350008 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969a
07:20:55.350012 SQW01       findComPtrFromId(): returning original INATEngine*=0x10220FB80 (IUnknown*=0x102600DD0)
07:20:55.350014 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.350121 SQW01       done calling COM method
07:20:55.350126 SQW01       convert COM output "redirects" back to caller format
07:20:55.350131 SQW01       done converting COM output "redirects" back to caller format
07:20:55.350136 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.350140 main     Pumping COM event queue
07:20:55.350587 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.350592 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019693
07:20:55.350596 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x106200270 (IUnknown*=0x102700100)
07:20:55.350599 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.350721 SQW01       done calling COM method
07:20:55.350728 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.350736 main     Pumping COM event queue
07:20:55.350842 main     Pumping COM event queue
07:20:55.350852 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x00000106300090 (IUnknown*=0x00000106300140; COM refcount now 3/2), new ID is 1969B; now 16 objects total
07:20:55.350860 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.350863 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.351321 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.351327 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969b
07:20:55.351331 SQW01       findComPtrFromId(): returning original INATEngine*=0x106300090 (IUnknown*=0x106300140)
07:20:55.351334 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.351433 SQW01       done calling COM method
07:20:55.351437 SQW01       convert COM output "hostIP" back to caller format
07:20:55.351442 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.351447 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.351452 main     Pumping COM event queue
07:20:55.351897 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.351903 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969b
07:20:55.351907 SQW01       findComPtrFromId(): returning original INATEngine*=0x106300090 (IUnknown*=0x106300140)
07:20:55.351909 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.352015 SQW01       done calling COM method
07:20:55.352025 main     Pumping COM event queue
07:20:55.352034 SQW01       convert COM output "redirects" back to caller format
07:20:55.352038 SQW01       done converting COM output "redirects" back to caller format
07:20:55.352042 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.352473 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.352479 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019694
07:20:55.352482 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x106102580 (IUnknown*=0x106200300)
07:20:55.352485 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.352594 SQW01       done calling COM method
07:20:55.352601 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.352606 main     Pumping COM event queue
07:20:55.352707 main     Pumping COM event queue
07:20:55.352715 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x00000102700910 (IUnknown*=0x0000010230a6c0; COM refcount now 3/2), new ID is 1969C; now 17 objects total
07:20:55.352722 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.352725 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.353184 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.353190 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969c
07:20:55.353194 SQW01       findComPtrFromId(): returning original INATEngine*=0x102700910 (IUnknown*=0x10230A6C0)
07:20:55.353196 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.353299 SQW01       done calling COM method
07:20:55.353307 SQW01       convert COM output "hostIP" back to caller format
07:20:55.353311 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.353315 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.353321 main     Pumping COM event queue
07:20:55.353772 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.353779 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969c
07:20:55.353783 SQW01       findComPtrFromId(): returning original INATEngine*=0x102700910 (IUnknown*=0x10230A6C0)
07:20:55.353786 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.353899 SQW01       done calling COM method
07:20:55.353904 SQW01       convert COM output "redirects" back to caller format
07:20:55.353906 SQW01       done converting COM output "redirects" back to caller format
07:20:55.353912 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.353917 main     Pumping COM event queue
07:20:55.354360 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.354365 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019695
07:20:55.354369 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x102605060 (IUnknown*=0x1026057A0)
07:20:55.354372 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.354485 SQW01       done calling COM method
07:20:55.354494 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.354501 main     Pumping COM event queue
07:20:55.354625 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x000001063002c0 (IUnknown*=0x000001026012e0; COM refcount now 3/2), new ID is 1969D; now 18 objects total
07:20:55.354634 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.354639 main     Pumping COM event queue
07:20:55.354647 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.355102 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.355108 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969d
07:20:55.355111 SQW01       findComPtrFromId(): returning original INATEngine*=0x1063002C0 (IUnknown*=0x1026012E0)
07:20:55.355114 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.355229 SQW01       done calling COM method
07:20:55.355237 SQW01       convert COM output "hostIP" back to caller format
07:20:55.355241 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.355245 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.355250 main     Pumping COM event queue
07:20:55.355704 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.355710 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969d
07:20:55.355713 SQW01       findComPtrFromId(): returning original INATEngine*=0x1063002C0 (IUnknown*=0x1026012E0)
07:20:55.355716 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.355822 SQW01       done calling COM method
07:20:55.355830 SQW01       convert COM output "redirects" back to caller format
07:20:55.355833 SQW01       done converting COM output "redirects" back to caller format
07:20:55.355837 main     Pumping COM event queue
07:20:55.355847 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.356287 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.356292 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019696
07:20:55.356295 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x102700880 (IUnknown*=0x1063008C0)
07:20:55.356298 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.356406 SQW01       done calling COM method
07:20:55.356419 main     Pumping COM event queue
07:20:55.356427 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.356544 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x00000102700430 (IUnknown*=0x0000010220f640; COM refcount now 3/2), new ID is 1969E; now 19 objects total
07:20:55.356554 main     Pumping COM event queue
07:20:55.356563 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.356567 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.357018 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.357024 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969e
07:20:55.357028 SQW01       findComPtrFromId(): returning original INATEngine*=0x102700430 (IUnknown*=0x10220F640)
07:20:55.357030 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.357143 SQW01       done calling COM method
07:20:55.357150 SQW01       convert COM output "hostIP" back to caller format
07:20:55.357154 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.357159 main     Pumping COM event queue
07:20:55.357167 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.357613 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.357619 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969e
07:20:55.357623 SQW01       findComPtrFromId(): returning original INATEngine*=0x102700430 (IUnknown*=0x10220F640)
07:20:55.357625 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.357733 SQW01       done calling COM method
07:20:55.357746 SQW01       convert COM output "redirects" back to caller format
07:20:55.357750 SQW01       done converting COM output "redirects" back to caller format
07:20:55.357754 main     Pumping COM event queue
07:20:55.357762 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.358214 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.358220 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019697
07:20:55.358224 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x10220F410 (IUnknown*=0x106102CD0)
07:20:55.358226 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.358343 SQW01       done calling COM method
07:20:55.358353 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.358359 main     Pumping COM event queue
07:20:55.358469 main     Pumping COM event queue
07:20:55.358479 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x00000106300410 (IUnknown*=0x000001063004a0; COM refcount now 3/2), new ID is 1969F; now 20 objects total
07:20:55.358486 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.358489 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.358938 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.358944 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969f
07:20:55.358947 SQW01       findComPtrFromId(): returning original INATEngine*=0x106300410 (IUnknown*=0x1063004A0)
07:20:55.358950 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.359054 SQW01       done calling COM method
07:20:55.359063 SQW01       convert COM output "hostIP" back to caller format
07:20:55.359068 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.359074 main     Pumping COM event queue
07:20:55.359085 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.359544 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.359550 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001969f
07:20:55.359554 SQW01       findComPtrFromId(): returning original INATEngine*=0x106300410 (IUnknown*=0x1063004A0)
07:20:55.359557 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.359678 SQW01       done calling COM method
07:20:55.359685 SQW01       convert COM output "redirects" back to caller format
07:20:55.359689 SQW01       done converting COM output "redirects" back to caller format
07:20:55.359694 main     Pumping COM event queue
07:20:55.359702 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.360145 SQW01    -- entering __vbox__INetworkAdapter_USCOREgetNATEngine
07:20:55.360150 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-0000000000019698
07:20:55.360154 SQW01       findComPtrFromId(): returning original INetworkAdapter*=0x1062004F0 (IUnknown*=0x102605500)
07:20:55.360157 SQW01       calling COM method COMGETTER(NATEngine)
07:20:55.360267 SQW01       done calling COM method
07:20:55.360275 SQW01       convert COM output "NATEngine" back to caller format
07:20:55.360281 main     Pumping COM event queue
07:20:55.360392 main     Pumping COM event queue
07:20:55.360402 SQW01       * ManagedObjectRef: MOR created for INATEngine*=0x00000102309280 (IUnknown*=0x00000102500850; COM refcount now 3/2), new ID is 196A0; now 21 objects total
07:20:55.360410 SQW01       done converting COM output "NATEngine" back to caller format
07:20:55.360413 SQW01    -- leaving __vbox__INetworkAdapter_USCOREgetNATEngine, rc: 0x0 (0)
07:20:55.360870 SQW01    -- entering __vbox__INATEngine_USCOREgetHostIP
07:20:55.360875 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-00000000000196a0
07:20:55.360879 SQW01       findComPtrFromId(): returning original INATEngine*=0x102309280 (IUnknown*=0x102500850)
07:20:55.360882 SQW01       calling COM method COMGETTER(HostIP)
07:20:55.360995 SQW01       done calling COM method
07:20:55.361004 SQW01       convert COM output "hostIP" back to caller format
07:20:55.361007 SQW01       done converting COM output "hostIP" back to caller format
07:20:55.361012 main     Pumping COM event queue
07:20:55.361020 SQW01    -- leaving __vbox__INATEngine_USCOREgetHostIP, rc: 0x0 (0)
07:20:55.361475 SQW01    -- entering __vbox__INATEngine_USCOREgetRedirects
07:20:55.361481 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-00000000000196a0
07:20:55.361485 SQW01       findComPtrFromId(): returning original INATEngine*=0x102309280 (IUnknown*=0x102500850)
07:20:55.361487 SQW01       calling COM method COMGETTER(Redirects)
07:20:55.361592 SQW01       done calling COM method
07:20:55.361596 SQW01       convert COM output "redirects" back to caller format
07:20:55.361607 main     Pumping COM event queue
07:20:55.361612 SQW01       done converting COM output "redirects" back to caller format
07:20:55.361619 SQW01    -- leaving __vbox__INATEngine_USCOREgetRedirects, rc: 0x0 (0)
07:20:55.362119 SQW01    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.362124 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968c
07:20:55.362129 SQW01       findComPtrFromId(): returning original ISession*=0x10230AD10 (IUnknown*=0x10230AD10)
07:20:55.362132 SQW01       calling COM method COMGETTER(Machine)
07:20:55.362237 SQW01       done calling COM method
07:20:55.362245 SQW01       convert COM output "machine" back to caller format
07:20:55.362250 main     Pumping COM event queue
07:20:55.362259 SQW01       findRefFromPtr: found existing ref 976d37b9f3f5d848-000000000001968f (IMachine) for COM obj 0x10230aed0
07:20:55.362263 SQW01       done converting COM output "machine" back to caller format
07:20:55.362307 SQW01    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.362748 SQW01    -- entering __vbox__IMachine_USCOREgetName
07:20:55.362754 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968f
07:20:55.362758 SQW01       findComPtrFromId(): returning original IMachine*=0x102210220 (IUnknown*=0x10230AED0)
07:20:55.362761 SQW01       calling COM method COMGETTER(Name)
07:20:55.362864 SQW01       done calling COM method
07:20:55.362870 main     Pumping COM event queue
07:20:55.362879 SQW01       convert COM output "name" back to caller format
07:20:55.362883 SQW01       done converting COM output "name" back to caller format
07:20:55.362886 SQW01    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.363497 SQW01    -- entering __vbox__ISession_USCOREunlockMachine
07:20:55.363503 SQW01       findRefFromId(): looking up objref 976d37b9f3f5d848-000000000001968c
07:20:55.363507 SQW01       findComPtrFromId(): returning original ISession*=0x10230AD10 (IUnknown*=0x10230AD10)
07:20:55.363510 SQW01       calling COM method UnlockMachine
07:20:55.363716 main     Pumping COM event queue
07:20:55.363811 main     Pumping COM event queue
07:20:55.363862 main     Pumping COM event queue
07:20:55.363913 main     Pumping COM event queue
07:20:55.363986 main     Pumping COM event queue
07:20:55.364024 SQW01       done calling COM method
07:20:55.364030 SQW01    -- leaving __vbox__ISession_USCOREunlockMachine, rc: 0x0 (0)
07:20:55.364861 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.364874 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1968C (ISession; COM refcount now 0/1); now 20 objects total
07:20:55.364879 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1968D (IVirtualBox; COM refcount now 1/2); now 19 objects total
07:20:55.364985 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1968E (IMachine; COM refcount now 0/0); now 18 objects total
07:20:55.365082 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1968F (IMachine; COM refcount now 0/0); now 17 objects total
07:20:55.365171 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19690 (ISystemProperties; COM refcount now 0/0); now 16 objects total
07:20:55.365272 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19691 (INetworkAdapter; COM refcount now 0/0); now 15 objects total
07:20:55.365360 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19692 (INetworkAdapter; COM refcount now 0/0); now 14 objects total
07:20:55.365436 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19693 (INetworkAdapter; COM refcount now 0/0); now 13 objects total
07:20:55.365517 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19694 (INetworkAdapter; COM refcount now 0/0); now 12 objects total
07:20:55.365615 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19695 (INetworkAdapter; COM refcount now 0/0); now 11 objects total
07:20:55.365705 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19696 (INetworkAdapter; COM refcount now 0/0); now 10 objects total
07:20:55.365791 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19697 (INetworkAdapter; COM refcount now 0/0); now 9 objects total
07:20:55.365885 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19698 (INetworkAdapter; COM refcount now 0/0); now 8 objects total
07:20:55.365973 SQW01       * ~ManagedObjectRef: deleting MOR for ID 19699 (INATEngine; COM refcount now 0/0); now 7 objects total
07:20:55.366051 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1969A (INATEngine; COM refcount now 0/0); now 6 objects total
07:20:55.366141 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1969B (INATEngine; COM refcount now 0/0); now 5 objects total
07:20:55.366229 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1969C (INATEngine; COM refcount now 0/0); now 4 objects total
07:20:55.366337 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1969D (INATEngine; COM refcount now 0/0); now 3 objects total
07:20:55.366428 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1969E (INATEngine; COM refcount now 0/0); now 2 objects total
07:20:55.366520 SQW01       * ~ManagedObjectRef: deleting MOR for ID 1969F (INATEngine; COM refcount now 0/0); now 1 objects total
07:20:55.366609 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A0 (INATEngine; COM refcount now 0/0); now 0 objects total
07:20:55.366621 SQW01    session destroyed, 0 sessions left open
07:20:55.366624 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.368005 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.368130 main     Pumping COM event queue
07:20:55.368140 SQW01       * ManagedObjectRef: MOR created for ISession*=0x000001062001c0 (IUnknown*=0x000001062001c0; COM refcount now 3/4), new ID is 196A1; now 1 objects total
07:20:55.368146 SQW01       * authenticate: created session object with comptr 0x000001062001c0, MOR = f81f6247f476b986-00000000000196a1
07:20:55.368198 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196A2; now 2 objects total
07:20:55.368203 SQW01    VirtualBox object ref is f81f6247f476b986-00000000000196a2
07:20:55.368206 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.368662 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.368668 SQW01       findRefFromId(): looking up objref f81f6247f476b986-00000000000196a2
07:20:55.368672 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.368676 SQW01       calling COM method FindMachine
07:20:55.368790 SQW01       done calling COM method
07:20:55.368800 SQW01       convert COM output "returnval" back to caller format
07:20:55.368806 main     Pumping COM event queue
07:20:55.368908 main     Pumping COM event queue
07:20:55.368918 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000106200310 (IUnknown*=0x00000102700100; COM refcount now 3/2), new ID is 196A3; now 3 objects total
07:20:55.368924 SQW01       done converting COM output "returnval" back to caller format
07:20:55.368927 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.369372 SQW01    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.369377 SQW01       findRefFromId(): looking up objref f81f6247f476b986-00000000000196a3
07:20:55.369381 SQW01       findComPtrFromId(): returning original IMachine*=0x106200310 (IUnknown*=0x102700100)
07:20:55.369384 SQW01       calling COM method GetExtraData
07:20:55.369503 SQW01       done calling COM method
07:20:55.369508 SQW01       convert COM output "returnval" back to caller format
07:20:55.369514 main     Pumping COM event queue
07:20:55.369524 SQW01       done converting COM output "returnval" back to caller format
07:20:55.369531 SQW01    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.370265 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.370278 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A1 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.370283 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A2 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.370387 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A3 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.370394 SQW01    session destroyed, 0 sessions left open
07:20:55.370396 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.378881 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.379178 main     Pumping COM event queue
07:20:55.379217 SQW01       * ManagedObjectRef: MOR created for ISession*=0x0000010230ac20 (IUnknown*=0x0000010230ac20; COM refcount now 3/4), new ID is 196A4; now 1 objects total
07:20:55.379227 SQW01       * authenticate: created session object with comptr 0x0000010230ac20, MOR = c5e28e3c02d2f49c-00000000000196a4
07:20:55.379352 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196A5; now 2 objects total
07:20:55.379359 SQW01    VirtualBox object ref is c5e28e3c02d2f49c-00000000000196a5
07:20:55.379362 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.385198 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.385542 main     Pumping COM event queue
07:20:55.385601 SQW01       * ManagedObjectRef: MOR created for ISession*=0x0000010230ad40 (IUnknown*=0x0000010230ad40; COM refcount now 3/4), new ID is 196A6; now 3 objects total
07:20:55.385622 SQW01       * authenticate: created session object with comptr 0x0000010230ad40, MOR = 48d158f1337325ba-00000000000196a6
07:20:55.385744 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 4/5), new ID is 196A7; now 4 objects total
07:20:55.385751 SQW01    VirtualBox object ref is 48d158f1337325ba-00000000000196a7
07:20:55.385754 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.386460 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.386466 SQW01       findRefFromId(): looking up objref 48d158f1337325ba-00000000000196a7
07:20:55.386481 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.386485 SQW01       calling COM method FindMachine
07:20:55.386713 SQW01       done calling COM method
07:20:55.386731 main     Pumping COM event queue
07:20:55.386741 SQW01       convert COM output "returnval" back to caller format
07:20:55.386962 main     Pumping COM event queue
07:20:55.386974 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x000001023099f0 (IUnknown*=0x00000106200000; COM refcount now 3/2), new ID is 196A8; now 5 objects total
07:20:55.386982 SQW01       done converting COM output "returnval" back to caller format
07:20:55.386985 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.387496 SQW01    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.387502 SQW01       findRefFromId(): looking up objref 48d158f1337325ba-00000000000196a8
07:20:55.387506 SQW01       findComPtrFromId(): returning original IMachine*=0x1023099F0 (IUnknown*=0x106200000)
07:20:55.387509 SQW01       calling COM method COMGETTER(Accessible)
07:20:55.387700 SQW01       done calling COM method
07:20:55.387707 SQW01       convert COM output "accessible" back to caller format
07:20:55.387709 main     Pumping COM event queue
07:20:55.387723 SQW01       done converting COM output "accessible" back to caller format
07:20:55.387728 SQW01    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.388640 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.388655 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A6 (ISession; COM refcount now 0/1); now 4 objects total
07:20:55.388660 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A7 (IVirtualBox; COM refcount now 2/3); now 3 objects total
07:20:55.388842 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A8 (IMachine; COM refcount now 0/0); now 2 objects total
07:20:55.388850 SQW01    session destroyed, 1 sessions left open
07:20:55.388852 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.390028 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.390041 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A4 (ISession; COM refcount now 0/1); now 1 objects total
07:20:55.390045 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A5 (IVirtualBox; COM refcount now 1/2); now 0 objects total
07:20:55.390049 SQW01    session destroyed, 0 sessions left open
07:20:55.390051 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.391625 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.391861 main     Pumping COM event queue
07:20:55.391884 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000106102480 (IUnknown*=0x00000106102480; COM refcount now 3/4), new ID is 196A9; now 1 objects total
07:20:55.391893 SQW01       * authenticate: created session object with comptr 0x00000106102480, MOR = 7be172f633daa234-00000000000196a9
07:20:55.391983 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196AA; now 2 objects total
07:20:55.391991 SQW01    VirtualBox object ref is 7be172f633daa234-00000000000196aa
07:20:55.392006 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.392563 SQW01    -- entering __vbox__IVirtualBox_USCOREgetMachinesByGroups
07:20:55.392570 SQW01       findRefFromId(): looking up objref 7be172f633daa234-00000000000196aa
07:20:55.392574 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.392578 SQW01       calling COM method GetMachinesByGroups
07:20:55.392815 SQW01       done calling COM method
07:20:55.392825 main     Pumping COM event queue
07:20:55.392832 SQW01       convert COM output "returnval" back to caller format
07:20:55.393039 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000102605310 (IUnknown*=0x00000102500850; COM refcount now 3/3), new ID is 196AB; now 3 objects total
07:20:55.393070 SQW01       done converting COM output "returnval" back to caller format
07:20:55.393093 SQW01    -- leaving __vbox__IVirtualBox_USCOREgetMachinesByGroups, rc: 0x0 (0)
07:20:55.393100 main     Pumping COM event queue
07:20:55.393648 SQW01    -- entering __vbox__IMachine_USCOREgetId
07:20:55.393654 SQW01       findRefFromId(): looking up objref 7be172f633daa234-00000000000196ab
07:20:55.393658 SQW01       findComPtrFromId(): returning original IMachine*=0x102605310 (IUnknown*=0x102500850)
07:20:55.393661 SQW01       calling COM method COMGETTER(Id)
07:20:55.393852 SQW01       done calling COM method
07:20:55.393858 SQW01       convert COM output "id" back to caller format
07:20:55.393861 SQW01       done converting COM output "id" back to caller format
07:20:55.393864 SQW01    -- leaving __vbox__IMachine_USCOREgetId, rc: 0x0 (0)
07:20:55.393877 main     Pumping COM event queue
07:20:55.394701 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.394714 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196A9 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.394719 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196AA (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.394900 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196AB (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.394909 SQW01    session destroyed, 0 sessions left open
07:20:55.394912 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.396356 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.396592 main     Pumping COM event queue
07:20:55.396617 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000106200090 (IUnknown*=0x00000106200090; COM refcount now 3/4), new ID is 196AC; now 1 objects total
07:20:55.396633 SQW01       * authenticate: created session object with comptr 0x00000106200090, MOR = 89981529202ae52e-00000000000196ac
07:20:55.396713 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196AD; now 2 objects total
07:20:55.396718 SQW01    VirtualBox object ref is 89981529202ae52e-00000000000196ad
07:20:55.396721 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.397252 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.397259 SQW01       findRefFromId(): looking up objref 89981529202ae52e-00000000000196ad
07:20:55.397263 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.397267 SQW01       calling COM method FindMachine
07:20:55.397504 main     Pumping COM event queue
07:20:55.397518 SQW01       done calling COM method
07:20:55.397524 SQW01       convert COM output "returnval" back to caller format
07:20:55.397680 main     Pumping COM event queue
07:20:55.397694 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x0000010220ff40 (IUnknown*=0x00000102700100; COM refcount now 3/2), new ID is 196AE; now 3 objects total
07:20:55.397699 SQW01       done converting COM output "returnval" back to caller format
07:20:55.397702 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.398210 SQW01    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.398216 SQW01       findRefFromId(): looking up objref 89981529202ae52e-00000000000196ae
07:20:55.398230 SQW01       findComPtrFromId(): returning original IMachine*=0x10220FF40 (IUnknown*=0x102700100)
07:20:55.398233 SQW01       calling COM method COMGETTER(Accessible)
07:20:55.398408 SQW01       done calling COM method
07:20:55.398432 main     Pumping COM event queue
07:20:55.398450 SQW01       convert COM output "accessible" back to caller format
07:20:55.398455 SQW01       done converting COM output "accessible" back to caller format
07:20:55.398458 SQW01    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.399273 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.399285 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196AC (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.399290 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196AD (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.399492 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196AE (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.399500 SQW01    session destroyed, 0 sessions left open
07:20:55.399503 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.400867 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.401076 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000102309c00 (IUnknown*=0x00000102309c00; COM refcount now 3/4), new ID is 196AF; now 1 objects total
07:20:55.401097 main     Pumping COM event queue
07:20:55.401101 SQW01       * authenticate: created session object with comptr 0x00000102309c00, MOR = 36bcd8496e814607-00000000000196af
07:20:55.401179 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196B0; now 2 objects total
07:20:55.401186 SQW01    VirtualBox object ref is 36bcd8496e814607-00000000000196b0
07:20:55.401189 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.401701 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.401706 SQW01       findRefFromId(): looking up objref 36bcd8496e814607-00000000000196b0
07:20:55.401710 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.401713 SQW01       calling COM method FindMachine
07:20:55.401942 SQW01       done calling COM method
07:20:55.401947 SQW01       convert COM output "returnval" back to caller format
07:20:55.401958 main     Pumping COM event queue
07:20:55.402079 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x0000010220f0d0 (IUnknown*=0x00000106300000; COM refcount now 3/2), new ID is 196B1; now 3 objects total
07:20:55.402093 main     Pumping COM event queue
07:20:55.402110 SQW01       done converting COM output "returnval" back to caller format
07:20:55.402120 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.402580 SQW01    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.402586 SQW01       findRefFromId(): looking up objref 36bcd8496e814607-00000000000196b1
07:20:55.402601 SQW01       findComPtrFromId(): returning original IMachine*=0x10220F0D0 (IUnknown*=0x106300000)
07:20:55.402603 SQW01       calling COM method COMGETTER(Accessible)
07:20:55.402738 SQW01       done calling COM method
07:20:55.402744 SQW01       convert COM output "accessible" back to caller format
07:20:55.402746 SQW01       done converting COM output "accessible" back to caller format
07:20:55.402749 main     Pumping COM event queue
07:20:55.402763 SQW01    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.403554 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.403577 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196AF (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.403581 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B0 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.403695 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B1 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.403702 SQW01    session destroyed, 0 sessions left open
07:20:55.403715 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.405084 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.405261 main     Pumping COM event queue
07:20:55.405285 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000102600e20 (IUnknown*=0x00000102600e20; COM refcount now 3/4), new ID is 196B2; now 1 objects total
07:20:55.405301 SQW01       * authenticate: created session object with comptr 0x00000102600e20, MOR = 8fa7bfae4e97c1d3-00000000000196b2
07:20:55.405362 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196B3; now 2 objects total
07:20:55.405369 SQW01    VirtualBox object ref is 8fa7bfae4e97c1d3-00000000000196b3
07:20:55.405374 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.405897 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.405905 SQW01       findRefFromId(): looking up objref 8fa7bfae4e97c1d3-00000000000196b3
07:20:55.405910 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.405914 SQW01       calling COM method FindMachine
07:20:55.406098 SQW01       done calling COM method
07:20:55.406108 SQW01       convert COM output "returnval" back to caller format
07:20:55.406124 main     Pumping COM event queue
07:20:55.406297 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000106102450 (IUnknown*=0x000001026050e0; COM refcount now 3/2), new ID is 196B4; now 3 objects total
07:20:55.406319 main     Pumping COM event queue
07:20:55.406336 SQW01       done converting COM output "returnval" back to caller format
07:20:55.406345 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.406867 SQW01    -- entering __vbox__IMachine_USCOREgetState
07:20:55.406874 SQW01       findRefFromId(): looking up objref 8fa7bfae4e97c1d3-00000000000196b4
07:20:55.406878 SQW01       findComPtrFromId(): returning original IMachine*=0x106102450 (IUnknown*=0x1026050E0)
07:20:55.406880 SQW01       calling COM method COMGETTER(State)
07:20:55.406993 SQW01       done calling COM method
07:20:55.406997 SQW01       convert COM output "state" back to caller format
07:20:55.407002 SQW01       done converting COM output "state" back to caller format
07:20:55.407007 SQW01    -- leaving __vbox__IMachine_USCOREgetState, rc: 0x0 (0)
07:20:55.407012 main     Pumping COM event queue
07:20:55.407843 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.407857 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B2 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.407861 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B3 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.407979 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B4 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.407985 SQW01    session destroyed, 0 sessions left open
07:20:55.407988 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.409620 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.409740 main     Pumping COM event queue
07:20:55.409746 SQW01       * ManagedObjectRef: MOR created for ISession*=0x0000010220d1c0 (IUnknown*=0x0000010220d1c0; COM refcount now 3/4), new ID is 196B5; now 1 objects total
07:20:55.409752 SQW01       * authenticate: created session object with comptr 0x0000010220d1c0, MOR = ee3240daddaf07ad-00000000000196b5
07:20:55.409817 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196B6; now 2 objects total
07:20:55.409823 SQW01    VirtualBox object ref is ee3240daddaf07ad-00000000000196b6
07:20:55.409826 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.410287 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.410292 SQW01       findRefFromId(): looking up objref ee3240daddaf07ad-00000000000196b6
07:20:55.410296 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.410300 SQW01       calling COM method FindMachine
07:20:55.410445 SQW01       done calling COM method
07:20:55.410453 SQW01       convert COM output "returnval" back to caller format
07:20:55.410470 main     Pumping COM event queue
07:20:55.410575 main     Pumping COM event queue
07:20:55.410581 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000106100030 (IUnknown*=0x0000010220d030; COM refcount now 3/2), new ID is 196B7; now 3 objects total
07:20:55.410587 SQW01       done converting COM output "returnval" back to caller format
07:20:55.410590 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.411047 SQW01    -- entering __vbox__IMachine_USCOREgetName
07:20:55.411054 SQW01       findRefFromId(): looking up objref ee3240daddaf07ad-00000000000196b7
07:20:55.411058 SQW01       findComPtrFromId(): returning original IMachine*=0x106100030 (IUnknown*=0x10220D030)
07:20:55.411060 SQW01       calling COM method COMGETTER(Name)
07:20:55.411197 SQW01       done calling COM method
07:20:55.411203 main     Pumping COM event queue
07:20:55.411215 SQW01       convert COM output "name" back to caller format
07:20:55.411220 SQW01       done converting COM output "name" back to caller format
07:20:55.411224 SQW01    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.411989 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.412002 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B5 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.412006 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B6 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.412123 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B7 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.412129 SQW01    session destroyed, 0 sessions left open
07:20:55.412132 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.413609 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.413754 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000102606340 (IUnknown*=0x00000102606340; COM refcount now 3/4), new ID is 196B8; now 1 objects total
07:20:55.413764 main     Pumping COM event queue
07:20:55.413773 SQW01       * authenticate: created session object with comptr 0x00000102606340, MOR = 456200b7da66f4cb-00000000000196b8
07:20:55.413845 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196B9; now 2 objects total
07:20:55.413849 SQW01    VirtualBox object ref is 456200b7da66f4cb-00000000000196b9
07:20:55.413852 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.414360 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.414366 SQW01       findRefFromId(): looking up objref 456200b7da66f4cb-00000000000196b9
07:20:55.414370 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.414374 SQW01       calling COM method FindMachine
07:20:55.414525 SQW01       done calling COM method
07:20:55.414534 SQW01       convert COM output "returnval" back to caller format
07:20:55.414541 main     Pumping COM event queue
07:20:55.414663 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x000001063000a0 (IUnknown*=0x00000102606840; COM refcount now 3/2), new ID is 196BA; now 3 objects total
07:20:55.414669 main     Pumping COM event queue
07:20:55.414678 SQW01       done converting COM output "returnval" back to caller format
07:20:55.414682 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.415221 SQW01    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.415227 SQW01       findRefFromId(): looking up objref 456200b7da66f4cb-00000000000196ba
07:20:55.415230 SQW01       findComPtrFromId(): returning original IMachine*=0x1063000A0 (IUnknown*=0x102606840)
07:20:55.415234 SQW01       calling COM method GetExtraData
07:20:55.415363 SQW01       done calling COM method
07:20:55.415375 main     Pumping COM event queue
07:20:55.415382 SQW01       convert COM output "returnval" back to caller format
07:20:55.415390 SQW01       done converting COM output "returnval" back to caller format
07:20:55.415395 SQW01    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.416198 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.416212 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B8 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.416217 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196B9 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.416355 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196BA (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.416364 SQW01    session destroyed, 0 sessions left open
07:20:55.416367 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.417917 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.418082 main     Pumping COM event queue
07:20:55.418089 SQW01       * ManagedObjectRef: MOR created for ISession*=0x000001063000a0 (IUnknown*=0x000001063000a0; COM refcount now 3/4), new ID is 196BB; now 1 objects total
07:20:55.418095 SQW01       * authenticate: created session object with comptr 0x000001063000a0, MOR = 2563084f8c277ca8-00000000000196bb
07:20:55.418168 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196BC; now 2 objects total
07:20:55.418174 SQW01    VirtualBox object ref is 2563084f8c277ca8-00000000000196bc
07:20:55.418177 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.418713 SQW01    -- entering __vbox__IWebsessionManager_USCOREgetSessionObject
07:20:55.418721 SQW01    -- leaving __vbox__IWebsessionManager_USCOREgetSessionObject, rc: 0x0
07:20:55.419220 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.419227 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bc
07:20:55.419231 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.419236 SQW01       calling COM method FindMachine
07:20:55.419397 SQW01       done calling COM method
07:20:55.419404 SQW01       convert COM output "returnval" back to caller format
07:20:55.419407 main     Pumping COM event queue
07:20:55.419541 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x0000010220ee00 (IUnknown*=0x00000106300380; COM refcount now 3/2), new ID is 196BD; now 3 objects total
07:20:55.419555 main     Pumping COM event queue
07:20:55.419569 SQW01       done converting COM output "returnval" back to caller format
07:20:55.419577 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.420071 SQW01    -- entering __vbox__IMachine_USCOREgetName
07:20:55.420077 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bd
07:20:55.420081 SQW01       findComPtrFromId(): returning original IMachine*=0x10220EE00 (IUnknown*=0x106300380)
07:20:55.420084 SQW01       calling COM method COMGETTER(Name)
07:20:55.420233 SQW01       done calling COM method
07:20:55.420238 main     Pumping COM event queue
07:20:55.420250 SQW01       convert COM output "name" back to caller format
07:20:55.420259 SQW01       done converting COM output "name" back to caller format
07:20:55.420265 SQW01    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.420960 SQW01    -- entering __vbox__IMachine_USCORElockMachine
07:20:55.420967 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bd
07:20:55.420971 SQW01       findComPtrFromId(): returning original IMachine*=0x10220EE00 (IUnknown*=0x106300380)
07:20:55.420974 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bb
07:20:55.420977 SQW01       findComPtrFromId(): returning original ISession*=0x1063000A0 (IUnknown*=0x1063000A0)
07:20:55.420980 SQW01       calling COM method LockMachine
07:20:55.421145 main     Pumping COM event queue
07:20:55.421259 main     Pumping COM event queue
07:20:55.421495 main     Pumping COM event queue
07:20:55.421632 main     Pumping COM event queue
07:20:55.421749 main     Pumping COM event queue
07:20:55.421861 SQW01       done calling COM method
07:20:55.421868 SQW01    -- leaving __vbox__IMachine_USCORElockMachine, rc: 0x0 (0)
07:20:55.421876 main     Pumping COM event queue
07:20:55.421938 main     Pumping COM event queue
07:20:55.422434 SQW01    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.422440 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bb
07:20:55.422444 SQW01       findComPtrFromId(): returning original ISession*=0x1063000A0 (IUnknown*=0x1063000A0)
07:20:55.422447 SQW01       calling COM method COMGETTER(Machine)
07:20:55.422562 SQW01       done calling COM method
07:20:55.422567 SQW01       convert COM output "machine" back to caller format
07:20:55.422571 main     Pumping COM event queue
07:20:55.422676 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000106101fe0 (IUnknown*=0x00000102700430; COM refcount now 3/3), new ID is 196BE; now 4 objects total
07:20:55.422686 main     Pumping COM event queue
07:20:55.422696 SQW01       done converting COM output "machine" back to caller format
07:20:55.422703 SQW01    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.423214 SQW01    -- entering __vbox__ISession_USCOREgetConsole
07:20:55.423220 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bb
07:20:55.423224 SQW01       findComPtrFromId(): returning original ISession*=0x1063000A0 (IUnknown*=0x1063000A0)
07:20:55.423227 SQW01       calling COM method COMGETTER(Console)
07:20:55.423481 SQW01       done calling COM method
07:20:55.423489 SQW01       convert COM output "console" back to caller format
07:20:55.423492 main     Pumping COM event queue
07:20:55.423603 SQW01       * ManagedObjectRef: MOR created for IConsole*=0x00000106102140 (IUnknown*=0x000001063005d0; COM refcount now 3/3), new ID is 196BF; now 5 objects total
07:20:55.423612 main     Pumping COM event queue
07:20:55.423621 SQW01       done converting COM output "console" back to caller format
07:20:55.423627 SQW01    -- leaving __vbox__ISession_USCOREgetConsole, rc: 0x0 (0)
07:20:55.424150 SQW01    -- entering __vbox__IConsole_USCOREgetMachine
07:20:55.424155 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bf
07:20:55.424159 SQW01       findComPtrFromId(): returning original IConsole*=0x106102140 (IUnknown*=0x1063005D0)
07:20:55.424162 SQW01       calling COM method COMGETTER(Machine)
07:20:55.424500 SQW01       done calling COM method
07:20:55.424511 SQW01       convert COM output "machine" back to caller format
07:20:55.424517 main     Pumping COM event queue
07:20:55.424526 SQW01       findRefFromPtr: found existing ref 2563084f8c277ca8-00000000000196be (IMachine) for COM obj 0x102700430
07:20:55.424533 SQW01       done converting COM output "machine" back to caller format
07:20:55.424590 SQW01    -- leaving __vbox__IConsole_USCOREgetMachine, rc: 0x0 (0)
07:20:55.425084 SQW01    -- entering __vbox__IMachine_USCOREgetGuestPropertyValue
07:20:55.425091 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196be
07:20:55.425095 SQW01       findComPtrFromId(): returning original IMachine*=0x106101FE0 (IUnknown*=0x102700430)
07:20:55.425099 SQW01       calling COM method GetGuestPropertyValue
07:20:55.425364 SQW01       done calling COM method
07:20:55.425373 SQW01       convert COM output "returnval" back to caller format
07:20:55.425378 SQW01       done converting COM output "returnval" back to caller format
07:20:55.425382 SQW01    -- leaving __vbox__IMachine_USCOREgetGuestPropertyValue, rc: 0x0 (0)
07:20:55.425387 main     Pumping COM event queue
07:20:55.425892 SQW01    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.425898 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bb
07:20:55.425902 SQW01       findComPtrFromId(): returning original ISession*=0x1063000A0 (IUnknown*=0x1063000A0)
07:20:55.425905 SQW01       calling COM method COMGETTER(Machine)
07:20:55.426023 SQW01       done calling COM method
07:20:55.426030 SQW01       convert COM output "machine" back to caller format
07:20:55.426040 main     Pumping COM event queue
07:20:55.426047 SQW01       findRefFromPtr: found existing ref 2563084f8c277ca8-00000000000196be (IMachine) for COM obj 0x102700430
07:20:55.426053 SQW01       done converting COM output "machine" back to caller format
07:20:55.426107 SQW01    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.426562 SQW01    -- entering __vbox__IMachine_USCOREgetName
07:20:55.426567 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196be
07:20:55.426571 SQW01       findComPtrFromId(): returning original IMachine*=0x106101FE0 (IUnknown*=0x102700430)
07:20:55.426573 SQW01       calling COM method COMGETTER(Name)
07:20:55.426691 SQW01       done calling COM method
07:20:55.426697 SQW01       convert COM output "name" back to caller format
07:20:55.426700 SQW01       done converting COM output "name" back to caller format
07:20:55.426706 SQW01    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.426711 main     Pumping COM event queue
07:20:55.427347 SQW01    -- entering __vbox__ISession_USCOREunlockMachine
07:20:55.427354 SQW01       findRefFromId(): looking up objref 2563084f8c277ca8-00000000000196bb
07:20:55.427358 SQW01       findComPtrFromId(): returning original ISession*=0x1063000A0 (IUnknown*=0x1063000A0)
07:20:55.427361 SQW01       calling COM method UnlockMachine
07:20:55.427588 main     Pumping COM event queue
07:20:55.427700 main     Pumping COM event queue
07:20:55.427743 main     Pumping COM event queue
07:20:55.427803 main     Pumping COM event queue
07:20:55.427879 main     Pumping COM event queue
07:20:55.427915 SQW01       done calling COM method
07:20:55.427920 SQW01    -- leaving __vbox__ISession_USCOREunlockMachine, rc: 0x0 (0)
07:20:55.428850 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.428866 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196BB (ISession; COM refcount now 0/1); now 4 objects total
07:20:55.428871 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196BC (IVirtualBox; COM refcount now 1/2); now 3 objects total
07:20:55.428993 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196BD (IMachine; COM refcount now 0/0); now 2 objects total
07:20:55.429110 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196BE (IMachine; COM refcount now 0/0); now 1 objects total
07:20:55.429219 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196BF (IConsole; COM refcount now 0/0); now 0 objects total
07:20:55.429229 SQW01    session destroyed, 0 sessions left open
07:20:55.429234 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.432517 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.432719 main     Pumping COM event queue
07:20:55.432732 SQW01       * ManagedObjectRef: MOR created for ISession*=0x0000010220f450 (IUnknown*=0x0000010220f450; COM refcount now 3/4), new ID is 196C0; now 1 objects total
07:20:55.432739 SQW01       * authenticate: created session object with comptr 0x0000010220f450, MOR = 63ed5bf67190f5e3-00000000000196c0
07:20:55.432800 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196C1; now 2 objects total
07:20:55.432806 SQW01    VirtualBox object ref is 63ed5bf67190f5e3-00000000000196c1
07:20:55.432809 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.434456 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.434629 main     Pumping COM event queue
07:20:55.434650 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000106300100 (IUnknown*=0x00000106300100; COM refcount now 3/4), new ID is 196C2; now 3 objects total
07:20:55.434656 SQW01       * authenticate: created session object with comptr 0x00000106300100, MOR = 4a5f802333c12927-00000000000196c2
07:20:55.434752 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 4/5), new ID is 196C3; now 4 objects total
07:20:55.434759 SQW01    VirtualBox object ref is 4a5f802333c12927-00000000000196c3
07:20:55.434762 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.435335 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.435341 SQW01       findRefFromId(): looking up objref 4a5f802333c12927-00000000000196c3
07:20:55.435346 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.435350 SQW01       calling COM method FindMachine
07:20:55.435526 SQW01       done calling COM method
07:20:55.435535 SQW01       convert COM output "returnval" back to caller format
07:20:55.435538 main     Pumping COM event queue
07:20:55.435682 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000102606a90 (IUnknown*=0x0000010220d210; COM refcount now 3/2), new ID is 196C4; now 5 objects total
07:20:55.435690 SQW01       done converting COM output "returnval" back to caller format
07:20:55.435697 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.435715 main     Pumping COM event queue
07:20:55.436225 SQW01    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.436231 SQW01       findRefFromId(): looking up objref 4a5f802333c12927-00000000000196c4
07:20:55.436235 SQW01       findComPtrFromId(): returning original IMachine*=0x102606A90 (IUnknown*=0x10220D210)
07:20:55.436238 SQW01       calling COM method COMGETTER(Accessible)
07:20:55.436414 main     Pumping COM event queue
07:20:55.436426 SQW01       done calling COM method
07:20:55.436431 SQW01       convert COM output "accessible" back to caller format
07:20:55.436433 SQW01       done converting COM output "accessible" back to caller format
07:20:55.436436 SQW01    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.437307 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.437323 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C2 (ISession; COM refcount now 0/1); now 4 objects total
07:20:55.437328 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C3 (IVirtualBox; COM refcount now 2/3); now 3 objects total
07:20:55.437468 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C4 (IMachine; COM refcount now 0/0); now 2 objects total
07:20:55.437477 SQW01    session destroyed, 1 sessions left open
07:20:55.437479 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.438337 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.438353 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C0 (ISession; COM refcount now 0/1); now 1 objects total
07:20:55.438358 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C1 (IVirtualBox; COM refcount now 1/2); now 0 objects total
07:20:55.438361 SQW01    session destroyed, 0 sessions left open
07:20:55.438363 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.439811 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.440012 main     Pumping COM event queue
07:20:55.440030 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000106100d50 (IUnknown*=0x00000106100d50; COM refcount now 3/4), new ID is 196C5; now 1 objects total
07:20:55.440038 SQW01       * authenticate: created session object with comptr 0x00000106100d50, MOR = 63ced16a67555a5a-00000000000196c5
07:20:55.440125 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196C6; now 2 objects total
07:20:55.440131 SQW01    VirtualBox object ref is 63ced16a67555a5a-00000000000196c6
07:20:55.440134 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.440689 SQW01    -- entering __vbox__IVirtualBox_USCOREgetMachinesByGroups
07:20:55.440696 SQW01       findRefFromId(): looking up objref 63ced16a67555a5a-00000000000196c6
07:20:55.440700 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.440704 SQW01       calling COM method GetMachinesByGroups
07:20:55.440867 SQW01       done calling COM method
07:20:55.440872 SQW01       convert COM output "returnval" back to caller format
07:20:55.440881 main     Pumping COM event queue
07:20:55.441014 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000102606d30 (IUnknown*=0x0000010220ee00; COM refcount now 3/3), new ID is 196C7; now 3 objects total
07:20:55.441026 main     Pumping COM event queue
07:20:55.441033 SQW01       done converting COM output "returnval" back to caller format
07:20:55.441043 SQW01    -- leaving __vbox__IVirtualBox_USCOREgetMachinesByGroups, rc: 0x0 (0)
07:20:55.441605 SQW01    -- entering __vbox__IMachine_USCOREgetId
07:20:55.441613 SQW01       findRefFromId(): looking up objref 63ced16a67555a5a-00000000000196c7
07:20:55.441617 SQW01       findComPtrFromId(): returning original IMachine*=0x102606D30 (IUnknown*=0x10220EE00)
07:20:55.441620 SQW01       calling COM method COMGETTER(Id)
07:20:55.441770 SQW01       done calling COM method
07:20:55.441783 main     Pumping COM event queue
07:20:55.441792 SQW01       convert COM output "id" back to caller format
07:20:55.441800 SQW01       done converting COM output "id" back to caller format
07:20:55.441804 SQW01    -- leaving __vbox__IMachine_USCOREgetId, rc: 0x0 (0)
07:20:55.442606 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.442621 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C5 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.442625 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C6 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.442776 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C7 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.442785 SQW01    session destroyed, 0 sessions left open
07:20:55.442788 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.444224 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.444388 main     Pumping COM event queue
07:20:55.444403 SQW01       * ManagedObjectRef: MOR created for ISession*=0x000001061008d0 (IUnknown*=0x000001061008d0; COM refcount now 3/4), new ID is 196C8; now 1 objects total
07:20:55.444407 SQW01       * authenticate: created session object with comptr 0x000001061008d0, MOR = af754048dd1e9599-00000000000196c8
07:20:55.444476 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196C9; now 2 objects total
07:20:55.444482 SQW01    VirtualBox object ref is af754048dd1e9599-00000000000196c9
07:20:55.444485 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.444980 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.444986 SQW01       findRefFromId(): looking up objref af754048dd1e9599-00000000000196c9
07:20:55.444990 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.444993 SQW01       calling COM method FindMachine
07:20:55.445135 SQW01       done calling COM method
07:20:55.445149 main     Pumping COM event queue
07:20:55.445163 SQW01       convert COM output "returnval" back to caller format
07:20:55.445304 main     Pumping COM event queue
07:20:55.445314 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x000001061004d0 (IUnknown*=0x0000010220d2a0; COM refcount now 3/2), new ID is 196CA; now 3 objects total
07:20:55.445322 SQW01       done converting COM output "returnval" back to caller format
07:20:55.445325 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.445849 SQW01    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.445856 SQW01       findRefFromId(): looking up objref af754048dd1e9599-00000000000196ca
07:20:55.445860 SQW01       findComPtrFromId(): returning original IMachine*=0x1061004D0 (IUnknown*=0x10220D2A0)
07:20:55.445863 SQW01       calling COM method COMGETTER(Accessible)
07:20:55.446002 SQW01       done calling COM method
07:20:55.446007 SQW01       convert COM output "accessible" back to caller format
07:20:55.446008 SQW01       done converting COM output "accessible" back to caller format
07:20:55.446016 main     Pumping COM event queue
07:20:55.446023 SQW01    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.446832 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.446845 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C8 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.446849 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196C9 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.446962 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196CA (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.446970 SQW01    session destroyed, 0 sessions left open
07:20:55.446973 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.448320 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.448468 SQW01       * ManagedObjectRef: MOR created for ISession*=0x000001026068d0 (IUnknown*=0x000001026068d0; COM refcount now 3/4), new ID is 196CB; now 1 objects total
07:20:55.448478 SQW01       * authenticate: created session object with comptr 0x000001026068d0, MOR = 765c69cce57d8b3e-00000000000196cb
07:20:55.448484 main     Pumping COM event queue
07:20:55.448546 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196CC; now 2 objects total
07:20:55.448554 SQW01    VirtualBox object ref is 765c69cce57d8b3e-00000000000196cc
07:20:55.448557 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.449020 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.449026 SQW01       findRefFromId(): looking up objref 765c69cce57d8b3e-00000000000196cc
07:20:55.449030 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.449033 SQW01       calling COM method FindMachine
07:20:55.449148 SQW01       done calling COM method
07:20:55.449154 main     Pumping COM event queue
07:20:55.449167 SQW01       convert COM output "returnval" back to caller format
07:20:55.449282 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000102500850 (IUnknown*=0x00000102606ef0; COM refcount now 3/2), new ID is 196CD; now 3 objects total
07:20:55.449290 main     Pumping COM event queue
07:20:55.449297 SQW01       done converting COM output "returnval" back to caller format
07:20:55.449302 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.449740 SQW01    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.449745 SQW01       findRefFromId(): looking up objref 765c69cce57d8b3e-00000000000196cd
07:20:55.449749 SQW01       findComPtrFromId(): returning original IMachine*=0x102500850 (IUnknown*=0x102606EF0)
07:20:55.449751 SQW01       calling COM method COMGETTER(Accessible)
07:20:55.449880 SQW01       done calling COM method
07:20:55.449884 SQW01       convert COM output "accessible" back to caller format
07:20:55.449886 SQW01       done converting COM output "accessible" back to caller format
07:20:55.449890 SQW01    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.449895 main     Pumping COM event queue
07:20:55.450672 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.450685 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196CB (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.450689 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196CC (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.450798 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196CD (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.450805 SQW01    session destroyed, 0 sessions left open
07:20:55.450807 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.452136 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.452256 main     Pumping COM event queue
07:20:55.452263 SQW01       * ManagedObjectRef: MOR created for ISession*=0x000001063000a0 (IUnknown*=0x000001063000a0; COM refcount now 3/4), new ID is 196CE; now 1 objects total
07:20:55.452271 SQW01       * authenticate: created session object with comptr 0x000001063000a0, MOR = 013f71a4ac70cd87-00000000000196ce
07:20:55.452331 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196CF; now 2 objects total
07:20:55.452337 SQW01    VirtualBox object ref is 013f71a4ac70cd87-00000000000196cf
07:20:55.452339 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.452807 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.452812 SQW01       findRefFromId(): looking up objref 013f71a4ac70cd87-00000000000196cf
07:20:55.452815 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.452819 SQW01       calling COM method FindMachine
07:20:55.452930 SQW01       done calling COM method
07:20:55.452936 SQW01       convert COM output "returnval" back to caller format
07:20:55.452938 main     Pumping COM event queue
07:20:55.453036 main     Pumping COM event queue
07:20:55.453046 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000102700150 (IUnknown*=0x00000102700430; COM refcount now 3/2), new ID is 196D0; now 3 objects total
07:20:55.453052 SQW01       done converting COM output "returnval" back to caller format
07:20:55.453055 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.453494 SQW01    -- entering __vbox__IMachine_USCOREgetState
07:20:55.453499 SQW01       findRefFromId(): looking up objref 013f71a4ac70cd87-00000000000196d0
07:20:55.453503 SQW01       findComPtrFromId(): returning original IMachine*=0x102700150 (IUnknown*=0x102700430)
07:20:55.453505 SQW01       calling COM method COMGETTER(State)
07:20:55.453642 SQW01       done calling COM method
07:20:55.453651 SQW01       convert COM output "state" back to caller format
07:20:55.453656 SQW01       done converting COM output "state" back to caller format
07:20:55.453661 SQW01    -- leaving __vbox__IMachine_USCOREgetState, rc: 0x0 (0)
07:20:55.453667 main     Pumping COM event queue
07:20:55.454486 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.454500 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196CE (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.454505 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196CF (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.454619 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196D0 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.454626 SQW01    session destroyed, 0 sessions left open
07:20:55.454629 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.455942 SQW01    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.456059 main     Pumping COM event queue
07:20:55.456065 SQW01       * ManagedObjectRef: MOR created for ISession*=0x00000106100520 (IUnknown*=0x00000106100520; COM refcount now 3/4), new ID is 196D1; now 1 objects total
07:20:55.456071 SQW01       * authenticate: created session object with comptr 0x00000106100520, MOR = eb03471b5780af3f-00000000000196d1
07:20:55.456127 SQW01       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196D2; now 2 objects total
07:20:55.456132 SQW01    VirtualBox object ref is eb03471b5780af3f-00000000000196d2
07:20:55.456135 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.456572 SQW01    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.456576 SQW01       findRefFromId(): looking up objref eb03471b5780af3f-00000000000196d2
07:20:55.456580 SQW01       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.456584 SQW01       calling COM method FindMachine
07:20:55.456696 SQW01       done calling COM method
07:20:55.456704 SQW01       convert COM output "returnval" back to caller format
07:20:55.456710 main     Pumping COM event queue
07:20:55.456830 main     Pumping COM event queue
07:20:55.456841 SQW01       * ManagedObjectRef: MOR created for IMachine*=0x00000106200030 (IUnknown*=0x00000106300000; COM refcount now 3/2), new ID is 196D3; now 3 objects total
07:20:55.456847 SQW01       done converting COM output "returnval" back to caller format
07:20:55.456851 SQW01    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.457281 SQW01    -- entering __vbox__IMachine_USCOREgetName
07:20:55.457286 SQW01       findRefFromId(): looking up objref eb03471b5780af3f-00000000000196d3
07:20:55.457290 SQW01       findComPtrFromId(): returning original IMachine*=0x106200030 (IUnknown*=0x106300000)
07:20:55.457293 SQW01       calling COM method COMGETTER(Name)
07:20:55.457409 SQW01       done calling COM method
07:20:55.457415 SQW01       convert COM output "name" back to caller format
07:20:55.457420 main     Pumping COM event queue
07:20:55.457427 SQW01       done converting COM output "name" back to caller format
07:20:55.457432 SQW01    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.458189 SQW01    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.458201 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196D1 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.458205 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196D2 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.458313 SQW01       * ~ManagedObjectRef: deleting MOR for ID 196D3 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.458321 SQW01    session destroyed, 0 sessions left open
07:20:55.458323 SQW01    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.459570 SQPmp    Request 2337 on socket 8 queued for processing (1 items on Q)
07:20:55.459575 SQW02    Processing connection from IP=127.0.0.1 socket=8 (1 out of 2 threads idle)
07:20:55.459753 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.459874 main     Pumping COM event queue
07:20:55.459882 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000102309620 (IUnknown*=0x00000102309620; COM refcount now 3/4), new ID is 196D4; now 1 objects total
07:20:55.459887 SQW02       * authenticate: created session object with comptr 0x00000102309620, MOR = 5b1a66a412231dfa-00000000000196d4
07:20:55.459947 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196D5; now 2 objects total
07:20:55.459952 SQW02    VirtualBox object ref is 5b1a66a412231dfa-00000000000196d5
07:20:55.459955 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.460410 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.460415 SQW02       findRefFromId(): looking up objref 5b1a66a412231dfa-00000000000196d5
07:20:55.460419 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.460423 SQW02       calling COM method FindMachine
07:20:55.460533 SQW02       done calling COM method
07:20:55.460540 SQW02       convert COM output "returnval" back to caller format
07:20:55.460545 main     Pumping COM event queue
07:20:55.460649 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102603680 (IUnknown*=0x00000102603710; COM refcount now 3/2), new ID is 196D6; now 3 objects total
07:20:55.460654 SQW02       done converting COM output "returnval" back to caller format
07:20:55.460657 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.460661 main     Pumping COM event queue
07:20:55.461118 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.461123 SQW02       findRefFromId(): looking up objref 5b1a66a412231dfa-00000000000196d6
07:20:55.461127 SQW02       findComPtrFromId(): returning original IMachine*=0x102603680 (IUnknown*=0x102603710)
07:20:55.461131 SQW02       calling COM method GetExtraData
07:20:55.461247 SQW02       done calling COM method
07:20:55.461258 main     Pumping COM event queue
07:20:55.461267 SQW02       convert COM output "returnval" back to caller format
07:20:55.461274 SQW02       done converting COM output "returnval" back to caller format
07:20:55.461278 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.462033 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.462045 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196D4 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.462050 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196D5 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.462152 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196D6 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.462160 SQW02    session destroyed, 0 sessions left open
07:20:55.462163 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.463484 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.463620 main     Pumping COM event queue
07:20:55.463628 SQW02       * ManagedObjectRef: MOR created for ISession*=0x000001023090a0 (IUnknown*=0x000001023090a0; COM refcount now 3/4), new ID is 196D7; now 1 objects total
07:20:55.463635 SQW02       * authenticate: created session object with comptr 0x000001023090a0, MOR = 0ec2b879ca9b214c-00000000000196d7
07:20:55.463688 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196D8; now 2 objects total
07:20:55.463692 SQW02    VirtualBox object ref is 0ec2b879ca9b214c-00000000000196d8
07:20:55.463695 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.464160 SQW02    -- entering __vbox__IWebsessionManager_USCOREgetSessionObject
07:20:55.464167 SQW02    -- leaving __vbox__IWebsessionManager_USCOREgetSessionObject, rc: 0x0
07:20:55.464605 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.464610 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d8
07:20:55.464614 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.464617 SQW02       calling COM method FindMachine
07:20:55.464736 SQW02       done calling COM method
07:20:55.464741 SQW02       convert COM output "returnval" back to caller format
07:20:55.464748 main     Pumping COM event queue
07:20:55.464848 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x000001062001a0 (IUnknown*=0x00000102309380; COM refcount now 3/2), new ID is 196D9; now 3 objects total
07:20:55.464857 SQW02       done converting COM output "returnval" back to caller format
07:20:55.464864 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.464874 main     Pumping COM event queue
07:20:55.465322 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.465328 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d9
07:20:55.465332 SQW02       findComPtrFromId(): returning original IMachine*=0x1062001A0 (IUnknown*=0x102309380)
07:20:55.465335 SQW02       calling COM method COMGETTER(Name)
07:20:55.465454 SQW02       done calling COM method
07:20:55.465463 SQW02       convert COM output "name" back to caller format
07:20:55.465468 SQW02       done converting COM output "name" back to caller format
07:20:55.465472 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.465477 main     Pumping COM event queue
07:20:55.466100 SQW02    -- entering __vbox__IMachine_USCORElockMachine
07:20:55.466106 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d9
07:20:55.466109 SQW02       findComPtrFromId(): returning original IMachine*=0x1062001A0 (IUnknown*=0x102309380)
07:20:55.466113 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d7
07:20:55.466116 SQW02       findComPtrFromId(): returning original ISession*=0x1023090A0 (IUnknown*=0x1023090A0)
07:20:55.466118 SQW02       calling COM method LockMachine
07:20:55.466235 main     Pumping COM event queue
07:20:55.466352 main     Pumping COM event queue
07:20:55.466555 main     Pumping COM event queue
07:20:55.466652 main     Pumping COM event queue
07:20:55.466759 main     Pumping COM event queue
07:20:55.466853 SQW02       done calling COM method
07:20:55.466863 main     Pumping COM event queue
07:20:55.466876 SQW02    -- leaving __vbox__IMachine_USCORElockMachine, rc: 0x0 (0)
07:20:55.466905 main     Pumping COM event queue
07:20:55.467325 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.467332 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d7
07:20:55.467336 SQW02       findComPtrFromId(): returning original ISession*=0x1023090A0 (IUnknown*=0x1023090A0)
07:20:55.467339 SQW02       calling COM method COMGETTER(Machine)
07:20:55.467448 SQW02       done calling COM method
07:20:55.467459 main     Pumping COM event queue
07:20:55.467468 SQW02       convert COM output "machine" back to caller format
07:20:55.467585 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100030 (IUnknown*=0x00000102601370; COM refcount now 3/3), new ID is 196DA; now 4 objects total
07:20:55.467593 SQW02       done converting COM output "machine" back to caller format
07:20:55.467598 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.467603 main     Pumping COM event queue
07:20:55.468062 SQW02    -- entering __vbox__ISession_USCOREgetConsole
07:20:55.468068 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d7
07:20:55.468071 SQW02       findComPtrFromId(): returning original ISession*=0x1023090A0 (IUnknown*=0x1023090A0)
07:20:55.468074 SQW02       calling COM method COMGETTER(Console)
07:20:55.468308 SQW02       done calling COM method
07:20:55.468316 SQW02       convert COM output "console" back to caller format
07:20:55.468321 main     Pumping COM event queue
07:20:55.468436 SQW02       * ManagedObjectRef: MOR created for IConsole*=0x00000106100910 (IUnknown*=0x00000106100150; COM refcount now 3/3), new ID is 196DB; now 5 objects total
07:20:55.468445 main     Pumping COM event queue
07:20:55.468453 SQW02       done converting COM output "console" back to caller format
07:20:55.468457 SQW02    -- leaving __vbox__ISession_USCOREgetConsole, rc: 0x0 (0)
07:20:55.468953 SQW02    -- entering __vbox__IConsole_USCOREgetMachine
07:20:55.468958 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196db
07:20:55.468962 SQW02       findComPtrFromId(): returning original IConsole*=0x106100910 (IUnknown*=0x106100150)
07:20:55.468964 SQW02       calling COM method COMGETTER(Machine)
07:20:55.469254 SQW02       done calling COM method
07:20:55.469260 SQW02       convert COM output "machine" back to caller format
07:20:55.469263 main     Pumping COM event queue
07:20:55.469271 SQW02       findRefFromPtr: found existing ref 0ec2b879ca9b214c-00000000000196da (IMachine) for COM obj 0x102601370
07:20:55.469275 SQW02       done converting COM output "machine" back to caller format
07:20:55.469325 SQW02    -- leaving __vbox__IConsole_USCOREgetMachine, rc: 0x0 (0)
07:20:55.469794 SQW02    -- entering __vbox__IMachine_USCOREgetGuestPropertyValue
07:20:55.469800 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196da
07:20:55.469804 SQW02       findComPtrFromId(): returning original IMachine*=0x106100030 (IUnknown*=0x102601370)
07:20:55.469808 SQW02       calling COM method GetGuestPropertyValue
07:20:55.470048 SQW02       done calling COM method
07:20:55.470053 main     Pumping COM event queue
07:20:55.470060 SQW02       convert COM output "returnval" back to caller format
07:20:55.470064 SQW02       done converting COM output "returnval" back to caller format
07:20:55.470068 SQW02    -- leaving __vbox__IMachine_USCOREgetGuestPropertyValue, rc: 0x0 (0)
07:20:55.470539 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.470544 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d7
07:20:55.470548 SQW02       findComPtrFromId(): returning original ISession*=0x1023090A0 (IUnknown*=0x1023090A0)
07:20:55.470551 SQW02       calling COM method COMGETTER(Machine)
07:20:55.470659 SQW02       done calling COM method
07:20:55.470667 SQW02       convert COM output "machine" back to caller format
07:20:55.470672 main     Pumping COM event queue
07:20:55.470681 SQW02       findRefFromPtr: found existing ref 0ec2b879ca9b214c-00000000000196da (IMachine) for COM obj 0x102601370
07:20:55.470685 SQW02       done converting COM output "machine" back to caller format
07:20:55.470738 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.471180 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.471185 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196da
07:20:55.471189 SQW02       findComPtrFromId(): returning original IMachine*=0x106100030 (IUnknown*=0x102601370)
07:20:55.471192 SQW02       calling COM method COMGETTER(Name)
07:20:55.471300 SQW02       done calling COM method
07:20:55.471305 SQW02       convert COM output "name" back to caller format
07:20:55.471308 SQW02       done converting COM output "name" back to caller format
07:20:55.471311 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.471319 main     Pumping COM event queue
07:20:55.471908 SQW02    -- entering __vbox__ISession_USCOREunlockMachine
07:20:55.471914 SQW02       findRefFromId(): looking up objref 0ec2b879ca9b214c-00000000000196d7
07:20:55.471918 SQW02       findComPtrFromId(): returning original ISession*=0x1023090A0 (IUnknown*=0x1023090A0)
07:20:55.471921 SQW02       calling COM method UnlockMachine
07:20:55.472112 main     Pumping COM event queue
07:20:55.472215 main     Pumping COM event queue
07:20:55.472259 main     Pumping COM event queue
07:20:55.472309 main     Pumping COM event queue
07:20:55.472366 main     Pumping COM event queue
07:20:55.472411 SQW02       done calling COM method
07:20:55.472415 SQW02    -- leaving __vbox__ISession_USCOREunlockMachine, rc: 0x0 (0)
07:20:55.473184 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.473196 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196D7 (ISession; COM refcount now 0/1); now 4 objects total
07:20:55.473201 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196D8 (IVirtualBox; COM refcount now 1/2); now 3 objects total
07:20:55.473315 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196D9 (IMachine; COM refcount now 0/0); now 2 objects total
07:20:55.473405 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196DA (IMachine; COM refcount now 0/0); now 1 objects total
07:20:55.473512 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196DB (IConsole; COM refcount now 0/0); now 0 objects total
07:20:55.473522 SQW02    session destroyed, 0 sessions left open
07:20:55.473526 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.476502 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.476683 main     Pumping COM event queue
07:20:55.476694 SQW02       * ManagedObjectRef: MOR created for ISession*=0x000001026013c0 (IUnknown*=0x000001026013c0; COM refcount now 3/4), new ID is 196DC; now 1 objects total
07:20:55.476701 SQW02       * authenticate: created session object with comptr 0x000001026013c0, MOR = 6d97771b109c24d4-00000000000196dc
07:20:55.476771 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196DD; now 2 objects total
07:20:55.476779 SQW02    VirtualBox object ref is 6d97771b109c24d4-00000000000196dd
07:20:55.476782 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.477336 SQW02    -- entering __vbox__IVirtualBox_USCOREgetMachinesByGroups
07:20:55.477343 SQW02       findRefFromId(): looking up objref 6d97771b109c24d4-00000000000196dd
07:20:55.477347 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.477352 SQW02       calling COM method GetMachinesByGroups
07:20:55.477508 SQW02       done calling COM method
07:20:55.477514 SQW02       convert COM output "returnval" back to caller format
07:20:55.477521 main     Pumping COM event queue
07:20:55.477632 main     Pumping COM event queue
07:20:55.477641 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100820 (IUnknown*=0x00000106100910; COM refcount now 3/3), new ID is 196DE; now 3 objects total
07:20:55.477650 SQW02       done converting COM output "returnval" back to caller format
07:20:55.477654 SQW02    -- leaving __vbox__IVirtualBox_USCOREgetMachinesByGroups, rc: 0x0 (0)
07:20:55.478197 SQW02    -- entering __vbox__IMachine_USCOREgetId
07:20:55.478203 SQW02       findRefFromId(): looking up objref 6d97771b109c24d4-00000000000196de
07:20:55.478206 SQW02       findComPtrFromId(): returning original IMachine*=0x106100820 (IUnknown*=0x106100910)
07:20:55.478209 SQW02       calling COM method COMGETTER(Id)
07:20:55.478327 SQW02       done calling COM method
07:20:55.478333 main     Pumping COM event queue
07:20:55.478341 SQW02       convert COM output "id" back to caller format
07:20:55.478345 SQW02       done converting COM output "id" back to caller format
07:20:55.478348 SQW02    -- leaving __vbox__IMachine_USCOREgetId, rc: 0x0 (0)
07:20:55.479066 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.479081 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196DC (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.479085 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196DD (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.479194 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196DE (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.479203 SQW02    session destroyed, 0 sessions left open
07:20:55.479215 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.480470 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.480604 main     Pumping COM event queue
07:20:55.480613 SQW02       * ManagedObjectRef: MOR created for ISession*=0x000001026013c0 (IUnknown*=0x000001026013c0; COM refcount now 3/4), new ID is 196DF; now 1 objects total
07:20:55.480619 SQW02       * authenticate: created session object with comptr 0x000001026013c0, MOR = d6ce25fd05238224-00000000000196df
07:20:55.480682 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196E0; now 2 objects total
07:20:55.480688 SQW02    VirtualBox object ref is d6ce25fd05238224-00000000000196e0
07:20:55.480691 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.481157 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.481163 SQW02       findRefFromId(): looking up objref d6ce25fd05238224-00000000000196e0
07:20:55.481167 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.481171 SQW02       calling COM method FindMachine
07:20:55.481289 SQW02       done calling COM method
07:20:55.481294 SQW02       convert COM output "returnval" back to caller format
07:20:55.481299 main     Pumping COM event queue
07:20:55.481400 main     Pumping COM event queue
07:20:55.481407 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100910 (IUnknown*=0x00000102309370; COM refcount now 3/2), new ID is 196E1; now 3 objects total
07:20:55.481414 SQW02       done converting COM output "returnval" back to caller format
07:20:55.481418 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.481878 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.481884 SQW02       findRefFromId(): looking up objref d6ce25fd05238224-00000000000196e1
07:20:55.481887 SQW02       findComPtrFromId(): returning original IMachine*=0x106100910 (IUnknown*=0x102309370)
07:20:55.481890 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.482001 SQW02       done calling COM method
07:20:55.482006 SQW02       convert COM output "accessible" back to caller format
07:20:55.482009 main     Pumping COM event queue
07:20:55.482017 SQW02       done converting COM output "accessible" back to caller format
07:20:55.482027 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.482722 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.482735 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196DF (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.482740 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E0 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.482857 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E1 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.482865 SQW02    session destroyed, 0 sessions left open
07:20:55.482867 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.484352 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.484491 main     Pumping COM event queue
07:20:55.484500 SQW02       * ManagedObjectRef: MOR created for ISession*=0x0000010220f360 (IUnknown*=0x0000010220f360; COM refcount now 3/4), new ID is 196E2; now 1 objects total
07:20:55.484505 SQW02       * authenticate: created session object with comptr 0x0000010220f360, MOR = 6ae119c4581b30ad-00000000000196e2
07:20:55.484566 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196E3; now 2 objects total
07:20:55.484573 SQW02    VirtualBox object ref is 6ae119c4581b30ad-00000000000196e3
07:20:55.484576 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.485051 SQW02    -- entering __vbox__IVirtualBox_USCOREgetMachinesByGroups
07:20:55.485058 SQW02       findRefFromId(): looking up objref 6ae119c4581b30ad-00000000000196e3
07:20:55.485062 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.485066 SQW02       calling COM method GetMachinesByGroups
07:20:55.485193 SQW02       done calling COM method
07:20:55.485198 SQW02       convert COM output "returnval" back to caller format
07:20:55.485205 main     Pumping COM event queue
07:20:55.485316 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102601370 (IUnknown*=0x00000106100d40; COM refcount now 3/3), new ID is 196E4; now 3 objects total
07:20:55.485325 main     Pumping COM event queue
07:20:55.485333 SQW02       done converting COM output "returnval" back to caller format
07:20:55.485338 SQW02    -- leaving __vbox__IVirtualBox_USCOREgetMachinesByGroups, rc: 0x0 (0)
07:20:55.485808 SQW02    -- entering __vbox__IMachine_USCOREgetId
07:20:55.485813 SQW02       findRefFromId(): looking up objref 6ae119c4581b30ad-00000000000196e4
07:20:55.485817 SQW02       findComPtrFromId(): returning original IMachine*=0x102601370 (IUnknown*=0x106100D40)
07:20:55.485819 SQW02       calling COM method COMGETTER(Id)
07:20:55.485930 SQW02       done calling COM method
07:20:55.485937 SQW02       convert COM output "id" back to caller format
07:20:55.485942 SQW02       done converting COM output "id" back to caller format
07:20:55.485949 main     Pumping COM event queue
07:20:55.485958 SQW02    -- leaving __vbox__IMachine_USCOREgetId, rc: 0x0 (0)
07:20:55.486644 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.486657 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E2 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.486661 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E3 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.486771 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E4 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.486777 SQW02    session destroyed, 0 sessions left open
07:20:55.486780 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.487985 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.488139 main     Pumping COM event queue
07:20:55.488148 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106100fc0 (IUnknown*=0x00000106100fc0; COM refcount now 3/4), new ID is 196E5; now 1 objects total
07:20:55.488154 SQW02       * authenticate: created session object with comptr 0x00000106100fc0, MOR = 06d803c4871d3518-00000000000196e5
07:20:55.488204 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196E6; now 2 objects total
07:20:55.488209 SQW02    VirtualBox object ref is 06d803c4871d3518-00000000000196e6
07:20:55.488212 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.488660 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.488667 SQW02       findRefFromId(): looking up objref 06d803c4871d3518-00000000000196e6
07:20:55.488671 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.488675 SQW02       calling COM method FindMachine
07:20:55.488790 SQW02       done calling COM method
07:20:55.488795 SQW02       convert COM output "returnval" back to caller format
07:20:55.488797 main     Pumping COM event queue
07:20:55.488893 main     Pumping COM event queue
07:20:55.488904 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106100150 (IUnknown*=0x00000102500850; COM refcount now 3/2), new ID is 196E7; now 3 objects total
07:20:55.488909 SQW02       done converting COM output "returnval" back to caller format
07:20:55.488912 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.489629 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.489634 SQW02       findRefFromId(): looking up objref 06d803c4871d3518-00000000000196e7
07:20:55.489638 SQW02       findComPtrFromId(): returning original IMachine*=0x106100150 (IUnknown*=0x102500850)
07:20:55.489641 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.489752 SQW02       done calling COM method
07:20:55.489757 SQW02       convert COM output "accessible" back to caller format
07:20:55.489762 SQW02       done converting COM output "accessible" back to caller format
07:20:55.489766 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.489771 main     Pumping COM event queue
07:20:55.490682 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.490696 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E5 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.490700 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E6 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.490802 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E7 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.490820 SQW02    session destroyed, 0 sessions left open
07:20:55.490824 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.492026 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.492144 main     Pumping COM event queue
07:20:55.492149 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106200070 (IUnknown*=0x00000106200070; COM refcount now 3/4), new ID is 196E8; now 1 objects total
07:20:55.492155 SQW02       * authenticate: created session object with comptr 0x00000106200070, MOR = b3940989d471f023-00000000000196e8
07:20:55.492206 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196E9; now 2 objects total
07:20:55.492211 SQW02    VirtualBox object ref is b3940989d471f023-00000000000196e9
07:20:55.492214 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.492664 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.492670 SQW02       findRefFromId(): looking up objref b3940989d471f023-00000000000196e9
07:20:55.492674 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.492678 SQW02       calling COM method FindMachine
07:20:55.492791 SQW02       done calling COM method
07:20:55.492801 SQW02       convert COM output "returnval" back to caller format
07:20:55.492807 main     Pumping COM event queue
07:20:55.492902 main     Pumping COM event queue
07:20:55.492914 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102500850 (IUnknown*=0x000001062007b0; COM refcount now 3/2), new ID is 196EA; now 3 objects total
07:20:55.492920 SQW02       done converting COM output "returnval" back to caller format
07:20:55.492923 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.493357 SQW02    -- entering __vbox__IMachine_USCOREgetAccessible
07:20:55.493362 SQW02       findRefFromId(): looking up objref b3940989d471f023-00000000000196ea
07:20:55.493365 SQW02       findComPtrFromId(): returning original IMachine*=0x102500850 (IUnknown*=0x1062007B0)
07:20:55.493368 SQW02       calling COM method COMGETTER(Accessible)
07:20:55.493473 SQW02       done calling COM method
07:20:55.493482 SQW02       convert COM output "accessible" back to caller format
07:20:55.493487 SQW02       done converting COM output "accessible" back to caller format
07:20:55.493493 main     Pumping COM event queue
07:20:55.493502 SQW02    -- leaving __vbox__IMachine_USCOREgetAccessible, rc: 0x0 (0)
07:20:55.494196 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.494210 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E8 (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.494214 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196E9 (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.494322 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196EA (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.494329 SQW02    session destroyed, 0 sessions left open
07:20:55.494331 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.495502 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.495627 main     Pumping COM event queue
07:20:55.495635 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000102604d60 (IUnknown*=0x00000102604d60; COM refcount now 3/4), new ID is 196EB; now 1 objects total
07:20:55.495641 SQW02       * authenticate: created session object with comptr 0x00000102604d60, MOR = 0d3f1b360643ffed-00000000000196eb
07:20:55.495689 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196EC; now 2 objects total
07:20:55.495695 SQW02    VirtualBox object ref is 0d3f1b360643ffed-00000000000196ec
07:20:55.495698 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.496166 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.496174 SQW02       findRefFromId(): looking up objref 0d3f1b360643ffed-00000000000196ec
07:20:55.496179 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.496184 SQW02       calling COM method FindMachine
07:20:55.496318 SQW02       done calling COM method
07:20:55.496326 SQW02       convert COM output "returnval" back to caller format
07:20:55.496331 main     Pumping COM event queue
07:20:55.496460 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x0000010220f320 (IUnknown*=0x00000106101040; COM refcount now 3/2), new ID is 196ED; now 3 objects total
07:20:55.496469 SQW02       done converting COM output "returnval" back to caller format
07:20:55.496474 main     Pumping COM event queue
07:20:55.496483 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.496977 SQW02    -- entering __vbox__IMachine_USCOREgetState
07:20:55.496986 SQW02       findRefFromId(): looking up objref 0d3f1b360643ffed-00000000000196ed
07:20:55.496992 SQW02       findComPtrFromId(): returning original IMachine*=0x10220F320 (IUnknown*=0x106101040)
07:20:55.496997 SQW02       calling COM method COMGETTER(State)
07:20:55.497134 SQW02       done calling COM method
07:20:55.497140 SQW02       convert COM output "state" back to caller format
07:20:55.497147 SQW02       done converting COM output "state" back to caller format
07:20:55.497153 main     Pumping COM event queue
07:20:55.497162 SQW02    -- leaving __vbox__IMachine_USCOREgetState, rc: 0x0 (0)
07:20:55.498011 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.498028 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196EB (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.498033 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196EC (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.498162 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196ED (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.498170 SQW02    session destroyed, 0 sessions left open
07:20:55.498172 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.499496 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.499658 main     Pumping COM event queue
07:20:55.499670 SQW02       * ManagedObjectRef: MOR created for ISession*=0x000001025008a0 (IUnknown*=0x000001025008a0; COM refcount now 3/4), new ID is 196EE; now 1 objects total
07:20:55.499678 SQW02       * authenticate: created session object with comptr 0x000001025008a0, MOR = d3fc04454cac82a4-00000000000196ee
07:20:55.499751 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196EF; now 2 objects total
07:20:55.499758 SQW02    VirtualBox object ref is d3fc04454cac82a4-00000000000196ef
07:20:55.499762 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.500281 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.500287 SQW02       findRefFromId(): looking up objref d3fc04454cac82a4-00000000000196ef
07:20:55.500290 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.500294 SQW02       calling COM method FindMachine
07:20:55.500429 SQW02       done calling COM method
07:20:55.500441 SQW02       convert COM output "returnval" back to caller format
07:20:55.500451 main     Pumping COM event queue
07:20:55.500573 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102500a60 (IUnknown*=0x000001063001b0; COM refcount now 3/2), new ID is 196F0; now 3 objects total
07:20:55.500584 main     Pumping COM event queue
07:20:55.500598 SQW02       done converting COM output "returnval" back to caller format
07:20:55.500602 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.501101 SQW02    -- entering __vbox__IMachine_USCOREgetExtraData
07:20:55.501107 SQW02       findRefFromId(): looking up objref d3fc04454cac82a4-00000000000196f0
07:20:55.501111 SQW02       findComPtrFromId(): returning original IMachine*=0x102500A60 (IUnknown*=0x1063001B0)
07:20:55.501114 SQW02       calling COM method GetExtraData
07:20:55.501239 SQW02       done calling COM method
07:20:55.501245 SQW02       convert COM output "returnval" back to caller format
07:20:55.501251 SQW02       done converting COM output "returnval" back to caller format
07:20:55.501256 main     Pumping COM event queue
07:20:55.501265 SQW02    -- leaving __vbox__IMachine_USCOREgetExtraData, rc: 0x0 (0)
07:20:55.502025 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.502040 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196EE (ISession; COM refcount now 0/1); now 2 objects total
07:20:55.502044 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196EF (IVirtualBox; COM refcount now 1/2); now 1 objects total
07:20:55.502174 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F0 (IMachine; COM refcount now 0/0); now 0 objects total
07:20:55.502183 SQW02    session destroyed, 0 sessions left open
07:20:55.502187 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.504404 SQW02    -- entering __vbox__IWebsessionManager_USCORElogon
07:20:55.504566 SQW02       * ManagedObjectRef: MOR created for ISession*=0x00000106100b20 (IUnknown*=0x00000106100b20; COM refcount now 3/4), new ID is 196F1; now 1 objects total
07:20:55.504573 main     Pumping COM event queue
07:20:55.504606 SQW02       * authenticate: created session object with comptr 0x00000106100b20, MOR = bc3f7732304b19b4-00000000000196f1
07:20:55.504684 SQW02       * ManagedObjectRef: MOR created for IVirtualBox*=0x00000102602a00 (IUnknown*=0x000001026038e0; COM refcount now 3/4), new ID is 196F2; now 2 objects total
07:20:55.504690 SQW02    VirtualBox object ref is bc3f7732304b19b4-00000000000196f2
07:20:55.504695 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogon, rc: 0x0
07:20:55.505339 SQW02    -- entering __vbox__IWebsessionManager_USCOREgetSessionObject
07:20:55.505348 SQW02    -- leaving __vbox__IWebsessionManager_USCOREgetSessionObject, rc: 0x0
07:20:55.505995 SQW02    -- entering __vbox__IVirtualBox_USCOREfindMachine
07:20:55.506001 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f2
07:20:55.506016 SQW02       findComPtrFromId(): returning original IVirtualBox*=0x102602A00 (IUnknown*=0x1026038E0)
07:20:55.506020 SQW02       calling COM method FindMachine
07:20:55.506234 SQW02       done calling COM method
07:20:55.506246 main     Pumping COM event queue
07:20:55.506263 SQW02       convert COM output "returnval" back to caller format
07:20:55.506450 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000102604e70 (IUnknown*=0x00000106300000; COM refcount now 3/2), new ID is 196F3; now 3 objects total
07:20:55.506500 main     Pumping COM event queue
07:20:55.506516 SQW02       done converting COM output "returnval" back to caller format
07:20:55.506526 SQW02    -- leaving __vbox__IVirtualBox_USCOREfindMachine, rc: 0x0 (0)
07:20:55.507074 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.507081 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f3
07:20:55.507085 SQW02       findComPtrFromId(): returning original IMachine*=0x102604E70 (IUnknown*=0x106300000)
07:20:55.507087 SQW02       calling COM method COMGETTER(Name)
07:20:55.507334 SQW02       done calling COM method
07:20:55.507340 SQW02       convert COM output "name" back to caller format
07:20:55.507347 SQW02       done converting COM output "name" back to caller format
07:20:55.507351 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x0 (0)
07:20:55.507369 main     Pumping COM event queue
07:20:55.508040 SQW02    -- entering __vbox__IMachine_USCORElockMachine
07:20:55.508047 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f3
07:20:55.508062 SQW02       findComPtrFromId(): returning original IMachine*=0x102604E70 (IUnknown*=0x106300000)
07:20:55.508065 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f1
07:20:55.508068 SQW02       findComPtrFromId(): returning original ISession*=0x106100B20 (IUnknown*=0x106100B20)
07:20:55.508070 SQW02       calling COM method LockMachine
07:20:55.508247 main     Pumping COM event queue
07:20:55.508364 main     Pumping COM event queue
07:20:55.508756 main     Pumping COM event queue
07:20:55.508877 main     Pumping COM event queue
07:20:55.509000 main     Pumping COM event queue
07:20:55.509100 SQW02       done calling COM method
07:20:55.509110 main     Pumping COM event queue
07:20:55.509118 SQW02    -- leaving __vbox__IMachine_USCORElockMachine, rc: 0x0 (0)
07:20:55.509144 main     Pumping COM event queue
07:20:55.509690 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.509696 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f1
07:20:55.509701 SQW02       findComPtrFromId(): returning original ISession*=0x106100B20 (IUnknown*=0x106100B20)
07:20:55.509704 SQW02       calling COM method COMGETTER(Machine)
07:20:55.509819 SQW02       done calling COM method
07:20:55.509829 SQW02       convert COM output "machine" back to caller format
07:20:55.509834 main     Pumping COM event queue
07:20:55.509937 main     Pumping COM event queue
07:20:55.509948 SQW02       * ManagedObjectRef: MOR created for IMachine*=0x00000106101f20 (IUnknown*=0x00000106200270; COM refcount now 3/3), new ID is 196F4; now 4 objects total
07:20:55.509955 SQW02       done converting COM output "machine" back to caller format
07:20:55.509959 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.510474 SQW02    -- entering __vbox__ISession_USCOREgetConsole
07:20:55.510480 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f1
07:20:55.510484 SQW02       findComPtrFromId(): returning original ISession*=0x106100B20 (IUnknown*=0x106100B20)
07:20:55.510487 SQW02       calling COM method COMGETTER(Console)
07:20:55.510754 SQW02       done calling COM method
07:20:55.510761 SQW02       convert COM output "console" back to caller format
07:20:55.510766 main     Pumping COM event queue
07:20:55.510891 SQW02       * ManagedObjectRef: MOR created for IConsole*=0x00000106101fb0 (IUnknown*=0x00000106102040; COM refcount now 3/3), new ID is 196F5; now 5 objects total
07:20:55.510902 main     Pumping COM event queue
07:20:55.510909 SQW02       done converting COM output "console" back to caller format
07:20:55.510914 SQW02    -- leaving __vbox__ISession_USCOREgetConsole, rc: 0x0 (0)
07:20:55.512103 SQW02    -- entering __vbox__IConsole_USCOREpowerDown
07:20:55.512108 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f5
07:20:55.512112 SQW02       findComPtrFromId(): returning original IConsole*=0x106101FB0 (IUnknown*=0x106102040)
07:20:55.512115 SQW02       calling COM method PowerDown
07:20:55.512846 SQW02       done calling COM method
07:20:55.512852 main     Pumping COM event queue
07:20:55.512862 SQW02       convert COM output "returnval" back to caller format
07:20:55.513000 main     Pumping COM event queue
07:20:55.513013 SQW02       * ManagedObjectRef: MOR created for IProgress*=0x00000102309e90 (IUnknown*=0x00000106200360; COM refcount now 3/2), new ID is 196F6; now 6 objects total
07:20:55.513018 SQW02       done converting COM output "returnval" back to caller format
07:20:55.513021 SQW02    -- leaving __vbox__IConsole_USCOREpowerDown, rc: 0x0 (0)
07:20:55.513820 SQW02    -- entering __vbox__IProgress_USCOREwaitForCompletion
07:20:55.513828 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f6
07:20:55.513832 SQW02       findComPtrFromId(): returning original IProgress*=0x102309E90 (IUnknown*=0x106200360)
07:20:55.513835 SQW02       calling COM method WaitForCompletion
07:20:55.645129 SQW02       done calling COM method
07:20:55.645151 main     Pumping COM event queue
07:20:55.645164 SQW02    -- leaving __vbox__IProgress_USCOREwaitForCompletion, rc: 0x0 (0)
07:20:55.646301 SQW02    -- entering __vbox__ISession_USCOREgetMachine
07:20:55.646309 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f1
07:20:55.646315 SQW02       findComPtrFromId(): returning original ISession*=0x106100B20 (IUnknown*=0x106100B20)
07:20:55.646318 SQW02       calling COM method COMGETTER(Machine)
07:20:55.646500 SQW02       done calling COM method
07:20:55.646509 SQW02       convert COM output "machine" back to caller format
07:20:55.646519 main     Pumping COM event queue
07:20:55.646533 SQW02       findRefFromPtr: found existing ref bc3f7732304b19b4-00000000000196f4 (IMachine) for COM obj 0x106200270
07:20:55.646542 SQW02       done converting COM output "machine" back to caller format
07:20:55.646606 SQW02    -- leaving __vbox__ISession_USCOREgetMachine, rc: 0x0 (0)
07:20:55.647026 main     Pumping COM event queue
07:20:55.647185 SQW02    -- entering __vbox__IMachine_USCOREgetName
07:20:55.647193 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f4
07:20:55.647200 SQW02       findComPtrFromId(): returning original IMachine*=0x106101F20 (IUnknown*=0x106200270)
07:20:55.647204 SQW02       calling COM method COMGETTER(Name)
07:20:55.647413 main     Pumping COM event queue
07:20:55.647600 main     Pumping COM event queue
07:20:55.647726 main     Pumping COM event queue
07:20:55.647882 main     Pumping COM event queue
07:20:55.648020 main     Pumping COM event queue
07:20:55.648188 main     Pumping COM event queue
07:20:55.648347 main     Pumping COM event queue
07:20:55.648482 main     Pumping COM event queue
07:20:55.648638 main     Pumping COM event queue
07:20:55.648710 main     Pumping COM event queue
07:20:55.648807 main     Pumping COM event queue
07:20:55.648929 SQW02       error, raising SOAP exception
07:20:55.648936 SQW02    API method name:            IMachine::COMGETTER(Name)
07:20:55.648939 SQW02    API return code:            0x80070005 (E_ACCESSDENIED)
07:20:55.648941 SQW02    COM error info result code: 0x80070005 (E_ACCESSDENIED)
07:20:55.648943 SQW02    COM error info text:        The object is not ready
07:20:55.649089 main     Pumping COM event queue
07:20:55.649209 main     Pumping COM event queue
07:20:55.649220 SQW02       * ManagedObjectRef: MOR created for IVirtualBoxErrorInfo*=0x0000010220f860 (IUnknown*=0x00000102500f30; COM refcount now 4/2), new ID is 196F7; now 7 objects total
07:20:55.649233 SQW02    -- leaving __vbox__IMachine_USCOREgetName, rc: 0x80070005 (-2147024891)
07:20:55.685871 SQPmp    Request 2338 on socket 8 queued for processing (1 items on Q)
07:20:55.685906 SQW02    Processing connection from IP=127.0.0.1 socket=8 (1 out of 2 threads idle)
07:20:55.686224 SQW02    -- entering __vbox__IWebsessionManager_USCORElogoff
07:20:55.686244 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F1 (ISession; COM refcount now 0/1); now 6 objects total
07:20:55.686250 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F2 (IVirtualBox; COM refcount now 1/2); now 5 objects total
07:20:55.686398 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F3 (IMachine; COM refcount now 0/0); now 4 objects total
07:20:55.686538 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F4 (IMachine; COM refcount now 0/0); now 3 objects total
07:20:55.686669 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F5 (IConsole; COM refcount now 0/0); now 2 objects total
07:20:55.686775 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F6 (IProgress; COM refcount now 0/0); now 1 objects total
07:20:55.686928 SQW02       * ~ManagedObjectRef: deleting MOR for ID 196F7 (IVirtualBoxErrorInfo; COM refcount now 0/0); now 0 objects total
07:20:55.686938 SQW02    session destroyed, 0 sessions left open
07:20:55.686941 SQW02    -- leaving __vbox__IWebsessionManager_USCORElogoff, rc: 0x0
07:20:55.689334 SQW02    -- entering __vbox__IVirtualBoxErrorInfo_USCOREgetText
07:20:55.689340 SQW02       findRefFromId(): looking up objref bc3f7732304b19b4-00000000000196f7
07:20:55.689342 SQW02       findRefFromId: cannot find session for objref bc3f7732304b19b4-00000000000196f7
07:20:55.689349 SQW02    -- leaving __vbox__IVirtualBoxErrorInfo_USCOREgetText, rc: 0xffffef32 (-4302)
07:21:16.012119 main     Pumping COM event queue
'''