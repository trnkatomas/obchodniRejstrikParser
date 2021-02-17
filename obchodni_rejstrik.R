library(xml2)
library(tidyverse)
library(parallel)

process_subject <- function(subject){
  name <- xml_text(xml_find_first(subject, "./nazev"))
  id <- xml_integer(xml_find_first(subject, "./ico"))
  partners <- process_entries(subject)
  return(partners %>% add_column(daughter_company_name=name, daughter_company_id=id))
}                         

process_entries <- function(subject){
  entries <- xml_find_all(subject, "./udaje/Udaj")
  partners <- Filter(function(x) xml_text(xml_find_first(x, "./hlavicka")) == "Společníci", entries)
  partner_items <- xml_find_all(partners, "./podudaje/Udaj")
  col_names <- c('entry_header', 'itemType', 'company_name', 'company_id', 'moneyEntry', 'share', 'dateEntered', 'dateErased')
  partners_values <- bind_rows(lapply(partner_items, function(x) process_partner(x, col_names))) %>% filter(!is.na(company_id))
  return(partners_values)
}

process_partner <- function(partner, col_names){
  entry_header <- xml_text(xml_find_first(partner, "./hlavicka"))
  itemType <- xml_text(xml_find_first(partner, "./hodnotaUdaje/typ"))
  company_name <- xml_text(xml_find_first(partner, "./osoba/nazev"))
  company_id <- xml_integer(xml_find_first(partner, "./osoba/ico"))
  moneyEntry <- xml_text(xml_find_first(partner,  ".//vklad/textValue"))
  share <- xml_text(xml_find_first(partner, ".//souhrn/textValue"))
  dateEntered <- xml_text(xml_find_first(partner, "./zapisDatum"))
  dateErased <- xml_text(xml_find_first(partner, "./vymazDatum"))
  data <- c(entry_header, itemType, company_name, company_id, moneyEntry, share, dateEntered, dateErased)
  return(as_tibble_row(set_names(data, col_names)))
}

# Read the whole xml file
data <- read_xml("/home/trnkat/Downloads/1k_sample.xml")
# parse the top level subject items
subjects <- xml_find_all(data, "//Subjekt")

# process all the items and create a tibble out of them
ptm <- proc.time()
# if you use supported system (need fork) use this
# bind_rows(mclapply(subjects, process_subject, mc.cores = 8)) %>% write_tsv("/home/trnkat/Downloads/1k_sample_R.tsv")
# else use non parallel version
bind_rows(lapply(subjects, process_subject)) %>% write_tsv("/home/trnkat/Downloads/1k_sample_R.tsv")
proc.time() - ptm
