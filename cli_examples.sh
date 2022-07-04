# create the database account
az cosmosdb create --name mdtest1234eu --kind GlobalDocumentDB --resource-group BigDataAcademyJuly2022

# list accounts (shows document endpoint uri)
az cosmosdb list -g BigDataAcademyJuly2022 -o table

# create the database
az cosmosdb sql database create --account-name mdtest1234 --name "Shop" --resource-group BigDataAcademyJuly2022

# create the container
az cosmosdb sql container create --account-name mdtest1234 --database-name "Shop" --name "Stock" --partition-key-path "/name" --throughput 400 --resource-group BigDataAcademyJuly2022
 
# show the database keys
az cosmosdb keys list --name mdtest1234 --resource-group BigDataAcademyJuly2022

# show the database connection strings
az cosmosdb keys list --type connection-strings --name mdtest1234 --resource-group BigDataAcademyJuly2022
