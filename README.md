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


    lein repl

    (init)
    (up)

But when we converge down to 0 nodes...

    (down)

... pallet-vmfest is told that VirtualBox has received an 'invalid managed object reference', to an object with an id of the form xxxxxxxxxxxxxxxx-0000000000000xxx, and the rest of the operation fails.  See example logs at https://gist.github.com/jonoflayham/9774cf714c9049af2ac0.

The same thing happens with Clojure 1.5.1 and VirtualBox 4.3.4.