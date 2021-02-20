# based on https://gist.github.com/Remiii/5429429

# Use at your own risk, think before using.
# Usage:
#    - Replace "myNumberOfNodesByOutputFile" by your number of nodes by output file
#    - Replace "myChildNode" by your tag
#    - Replace "myParentNode" by your parent tag
# Run:
#    $ awk -f xml-split.awk myXML.xml

BEGIN { count=0 }
/<Subjekt>/ {
        rfile=FILENAME "-part-" count ".xml"
        print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > rfile
        print "<xml>" > rfile
        print $0 > rfile
        for ( iLoop=0 ; iLoop<30000 ; iLoop++ )
        {
            getline
            while ( ( $0 !~ "</Subjekt>" ) && ( $0 !~ "</xml>" ) )
            {
                print > rfile
                getline
            }
            if ( $0 !~ "</xml>" )
            {
                print $0 > rfile
            }
        }
        print "</xml>" > rfile
        close(rfile)
        count++
}
work