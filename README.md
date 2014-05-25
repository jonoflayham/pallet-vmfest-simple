pallet-vmfest-simple
====================

A simple pallet-vmfest project for problem-solving.

The current problem: having converged up to 1 node, can't converge back down to 0
---------------------------------------------------------------------------------

We've created a CentOS 6.5 vdi multi-attach image with two DHCPed network interfaces and not much else, following the approach at eg http://pepijndevos.nl/2013/03/20/vmfest-base-image.html.  We're tied to using CentOS 6.5 for various reasons.

We can converge up to 1 node, and everything works beautifully:


    lein repl

    (init)
    (up)

But when we converge down to 0 nodes...

    (down)

... this is the result:

<script src="https://gist.github.com/jonoflayham/9774cf714c9049af2ac0.js"></script>
