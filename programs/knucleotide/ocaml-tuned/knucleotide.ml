(* The Computer Language Benchmarks Game
 * https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
 *
 * contributed by Troestler Christophe
 * modified by Mauricio Fernandez
 * fixed by glacialthinker
 *
 * Keyed by each k-mer's 2k-bit code rather than by its string, as every program in this suite
 * is; the reading, sorting and output are the archive program's.
 *)

module H = Hashtbl.Make (struct
  type t = int
  let equal (a : int) b = a = b
  let hash (k : int) = Hashtbl.hash k
end)

let code = function 'a' | 'A' -> 0 | 'c' | 'C' -> 1 | 'g' | 'G' -> 2 | _ -> 3

let decode key k = String.init k (fun i -> "ACGT".[(key lsr (2 * (k - 1 - i))) land 3])

(* [counts k dna] counts every k-mer of [dna]; keys point to mutable values, so a k-mer seen
   before is found once. *)
let count = H.create 0x40000
let counts k dna =
  H.clear count;
  let mask = (1 lsl (2 * k)) - 1 in
  let key = ref 0 in
  for i = 0 to Bytes.length dna - 1 do
    key := ((!key lsl 2) land mask) lor Char.code (Bytes.unsafe_get dna i);
    if i >= k - 1 then
      try incr (H.find count !key) with Not_found -> H.add count !key (ref 1)
  done;
  count

let compare_freq ((k1 : int), (f1 : float)) (k2, f2) =
  if f1 > f2 then -1 else if f1 < f2 then 1 else compare k1 k2

let write_frequencies k dna =
  let cnt = counts k dna in
  let tot = float (H.fold (fun _ n t -> !n + t) cnt 0) in
  let frq = H.fold (fun k n l -> (k, 100. *. float !n /. tot) :: l) cnt [] in
  let frq = List.sort compare_freq frq in
  List.iter (fun (key, f) -> Printf.printf "%s %.3f\n" (decode key k) f) frq;
  print_string "\n"

let write_count seq dna =
  let key = ref 0 in
  String.iter (fun c -> key := (!key lsl 2) lor code c) seq;
  let cnt = counts (String.length seq) dna in
  Printf.printf "%d\t%s\n" (try !(H.find cnt !key) with Not_found -> 0) seq

(* Extract DNA sequence "THREE" from stdin, as one code per byte *)
let dna_three =
  let is_not_three s = try String.sub s 0 6 <> ">THREE" with _ -> true in
  while is_not_three (input_line stdin) do () done;
  let buf = Buffer.create 1000 in
  (try
     while true do
       let line = input_line stdin in
       if line.[0] = '>' then raise End_of_file;
       String.iter (fun c -> Buffer.add_char buf (Char.unsafe_chr (code c))) line
     done
   with End_of_file -> ());
  Buffer.to_bytes buf

let () = Gc.set { (Gc.get ()) with Gc.minor_heap_size = 1024 * 2048 }

let () =
  List.iter (fun i -> write_frequencies i dna_three) [ 1; 2 ];
  List.iter (fun k -> write_count k dna_three)
    [ "GGT"; "GGTA"; "GGTATT"; "GGTATTTTAATT"; "GGTATTTTAATTTATAGT" ]
