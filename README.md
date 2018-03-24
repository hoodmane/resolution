# resolution
Variations on a theme: the cohomology of the Steenrod algebra, providing the E_2 page of the Adams spectral sequence.

This program computes Ext_A(M,F_p), where

* *A* is the Steenrod algebra or its subalgebra A(n) or the even subalgebra P,
* *M* is an A-module
* *p* is a prime.

Output takes the form of a scrollable chart, with products by the first three Hopf elements drawn (even at odd primes where they are not actually spherical homotopy classes).
There are also various output formats.



# How to use

Running resolution requires only the file `resolution.jar`, available at 

http://github.com/hoodmane/resolution/resolution.jar

To run the program, call `java -jar resolution.jar inputFile.json`, or if this is too long for you and you have a unix-style system (or windows and Cygwin bash), 
run `./resolution inputFile.json`.

For trigraded computations in the 2D viewer, there are controls to limit the visible range for the third grading; these are also jointly controlled by the PgUp and PgDn keys on your keyboard, in case you want to quickly step through cross-sections.

If there are features you'd like to see in this program, please get in touch!


# Input format

Input is a json file. The simplest possible file that will run is `{prime: 2}` which will resolve the sphere at 2. 
The "modules" directory contains a bunch of working json files for standard Steenrod modules which should get you started.

Fields:

- `prime`: An integer, the prime that you are working at. This field is mandatory, if it is missing resolution will quit with an error.

- `generators`: A map <span style="background:rgba(0,0,0,0.04)"> _String_ &rarr; _Int_</span> of the form <span style="background:rgba(0,0,0,0.04)">_generatorName_ &map; _generator degree_</span>. For example, `"generators" : {"x0" : 0, "x2" : 2}`. If this field is omitted, it defaults to a single generator in degree zero (so computing the E_2 page for the sphere).
   
- `relations`: A list of relations. If this field is omitted, then the Steenrod action is assumed to vanish (so computing the E_2 page for a wedge of spheres). Note that resolution makes no attempt to check whether  Each relation is a string. The format the string must have is a bit complicated. First I'll give an example: `"relations" : ["Sq2(x0) = x2"]` indicates that a `Sq2` connects `x0` to `x2`. It should have the form `<Operator>(<var>) = <linear combination>`. Here `<var>`is one of the generator names specified in the generators field. `<Operator>` is of one of the forms `Sq<n>`, `b`, `P<n>` or `bP<n>`. All of these work at both 2 and odd primes. At 2, `Pn` is the same as `Sq2*n` and `bPn` is the same as `Sq2*n+1`. At odd primes, `Sqn` refers to the unique generator that raises degree by n if one exists and is illegal otherwise (so to be legal, `n` should be congruent to 0 or 1 mod 2*p - 2). The right hand side should be a valid linear combination of appropriate degree variables. This is expressed in terms of functions `+/-` (which take two vectors or two scalars), `*` (takes a vector and a scalar), `binom(m,n)` and `n!` which take scalars, and the function `sum(expr,{i,imin,imax,istep})` which expands to the sum of `expr` over the iterator. There is also the function `table` which produces multiple relations using an iterator.

- `T_max`: A positive integer. The maximum value of t to compute, where the ASS has the form Ext^{s,t}(HF_p_* M) ==> pi_{t-s}(M). Defaults to 50.

- `xscale` : A double. Adjusts the `xscale` of the viewer. Defaults to 1. Note that the y/x aspect ratio currently can't be adjusted in the GUI.

- `yscale` : A double. Adjusts the `yscale` of the viewer. Defaults to |v1| = 2*p-2. Note that the y/x aspect ratio currently can't be adjusted in the GUI.

- `scale` : Adjust the starting scale of the viewer. Defaults to 1. This can be adjusted by scrolling in the GUI.

- `tex_output` : A string filename. Makes a file with the given name with `spectralsequences` tex code to display the computed spectral sequence drawing. For large drawings, the code may run slowly if at all. Compile with lualatex for best results.

- `json_output` : A string filename. Writes the calculated spectral sequence into a json file. This file can be viewed later using resolution without recomputing the resolution page. Note that this is not stored in a way that facilitates computing more stems -- so far if you want that, everything needs to be recomputed.

- `windowed` : A boolean. If this is false, then no gui will open. In that case, in order for the program to do anything, at least one of the `tex_output` or `json_output` fields need to be present. Useful for running computations on remote computers.

# Compiling
The easy way to compile is to import the folder as a netbeans repository. There is also a make script to help compile by hand, but it's a bit more finnicky.


# Acknowledgements

#### Amelia Perry:
Most of the features in this program were developed during a period of collaboration with Michael Andrews, 
and so he has guided its direction to a large extent. Thanks also to Mark Behrens for feature suggestions.

