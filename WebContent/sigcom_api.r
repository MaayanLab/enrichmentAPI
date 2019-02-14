
setwd("~/OneDrive/sigcommons")
library("rhdf5")
library("rjson")
library("httr")


sigs = c("aa68fb9a-52e5-42e2-a127-1b7f245b4007","f2c27842-9b6e-4508-93f4-3adb8c91acf2","c7108868-aacf-4354-9773-9e37d0d88c6b","27cadffc-c329-4f41-9490-a6abfb116751","eb955dd0-5fd6-4937-a9ba-b11f14845f0e","9951567c-8d1e-4917-bbd6-24805532f77a","ba12f83f-8ab1-46b5-bae6-9499aed06c5b","7c60caaa-fd4b-4822-863a-f3d87bb9e67f")

sigs = sig_uid[100:300,2]

url = "https://amp.pharm.mssm.edu/enrichmentapi/api/fetch/rank"

postdata <- list(
  entities = c(),
  signatures = sigs,
  database = "lincsfwd"
)

res <- POST(url, body = postdata, encode = "json", verbose())

result = content(res)
entities = unlist(result["entities"])

s1 = list()
for(i in 1:length(result["signatures"][[1]])){
    print(i)
    uid = unlist(result["signatures"][[1]][[i]]["uid"])
    ranks = unlist(result["signatures"][[1]][[i]]["ranks"])
    names(ranks) = entities
    s1[[uid]] = c(ranks)
}
fwd_mat = do.call(cbind, s1)


oo = order(fwd_mat[,1])
top20 = rownames(fwd_mat)[oo[1:20]]
bottom20 = rownames(fwd_mat)[rev(oo)[1:20]]


