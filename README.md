# obchodniRejstrikParser
Parsovani OR 

# Build
Requirements `sbt`
you can build a self-contained JAR with `sbt assembly` called in te root folder. 
The final jar can be found in the target directory (for regular users, the jar can be found attached to a release).

# Usage
If you have recent enough java (you should have at least Java 8), you shoudl be able to run the program from a command line
as follows:
`java -jar ObchodniRejstrik-assembly-*.jar -h`
This will show the available options for the input and the output. 

# Get the data
```bash
export base_url="https://dataor.justice.cz/api/file"
parallel --bar "wget -q \$base_url/{1}-full-{2}-{3}.xml.gz" ::: as sro ::: praha ceske_budejovice plzen usti_nad_labem hradec_kralove brno ostrava ::: $(seq 2005 2021)
``` 

# Prepare and process the data
The small files should be just fine, be small I mean xml files containing <= 30k `<Subjekt>` elements, the bigger ones needs some processing.
The example of such processing can be seen in [workflow.sh](workflow.sh)
For small files run just:
```bash
java -jar ObchodniRejstrik-assembly-*.jar -c -t sro -i sro-full-usti_nad_labem-2015.xml.gz -o  sro-full-usti_nad_labem-2015.tsv
```
The `-c` means that it await the input being compressed, the `-t` parameter chooses between `s.r.o.` and `a.s.` inputs, the `-i` is input file,
the `-o` determines the output file. 

# Nice to haves
- [ ] parse the online data from [http://wwwinfo.mfcr.cz/cgi-bin/ares/darv_or.cgi?ico=] (beware of rate limiting and other fun stuff)
- [ ] more customizable queries