find data -name "sro*.gz" | parallel echo "{}: \$(cat {} | gunzip | grep '</Subjekt>' | wc -l)" > sro_number_of_subjects.txt

max_objects=30000
cat sro_number_of_subjects.txt | awk -F '[:]' -v max_objects=$max_objects '{if ($2 < max_objects){print $1}}' > sro_small_files.txt
cat sro_number_of_subjects.txt | awk -F '[:]' -v max_objects=$max_objects '{if ($2 > max_objects){print $1}}' > sro_to_split.txt

cat sro_small_files.txt | parallel -j 2 "java -jar ObchodniRejstrik-assembly-0.0.1.jar -c -t \$(echo {/}|cut -f1 -d'-') -i {} -o {}.tsv"

function unpack_split_pack {
  gunzip -c $1 > $1.xml
  awk -f xml-split.awk $1.xml
  gzip $1.xml-part-*.xml
  find . -name "$1.xml-part-*.gz"
  rm $1.xml
}

cat sro_to_split.txt | parallel -j 2 "unpack_split_pack {}" # it's a good idea to limit the parallelization, huge decompress files can flood the HDD
find data/ -name "*-part-*xml.gz" | parallel -j 2 "java -jar ObchodniRejstrik-assembly-0.0.1.jar -c -t \$(echo {/}|cut -f1 -d'-') -i {} -o {}.tsv"