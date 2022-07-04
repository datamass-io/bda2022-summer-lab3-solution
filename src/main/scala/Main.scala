import com.typesafe.scalalogging.LazyLogging
import com.azure.cosmos.{ConsistencyLevel, CosmosClient, CosmosClientBuilder, CosmosContainer, CosmosDatabase, CosmosException}
import com.azure.cosmos.models.{CosmosContainerProperties, CosmosItemRequestOptions, CosmosQueryRequestOptions, FeedResponse, PartitionKey, ThroughputProperties}

import scala.jdk.CollectionConverters.SeqHasAsJava

object Main extends LazyLogging {

  private val accountHost = System.getenv("ACCOUNT_HOST")
  private val masterKey = System.getenv("ACCOUNT_KEY")

  private val databaseName = "Shop"
  private val containerName = "Stock"

  def initDB(): (CosmosClient, CosmosContainer) = {
    val client = new CosmosClientBuilder()
      .endpoint(accountHost).key(masterKey)
      .preferredRegions(List("South UK").asJava)
      .consistencyLevel(ConsistencyLevel.EVENTUAL).buildClient

    //  </CreateSyncClient>
    val database = createDatabaseIfNotExists(client)
    val container = createContainerIfNotExists(database)
    (client, container)
  }


  @throws[Exception]
  private def createDatabaseIfNotExists(client: CosmosClient): CosmosDatabase = {
    logger.info("Create database {} if not exists.", databaseName)
    //  Create database if not exists
    //  <CreateDatabaseIfNotExists>
    val cosmosDatabaseResponse = client.createDatabaseIfNotExists(databaseName)
    val database = client.getDatabase(cosmosDatabaseResponse.getProperties.getId)
    //  </CreateDatabaseIfNotExists>
    logger.info("Checking database {} completed!\n", database.getId)
    database
  }

  @throws[Exception]
  private def createContainerIfNotExists(database: CosmosDatabase): CosmosContainer = {
    logger.info("Create container {} if not exists.", containerName)
    //  Create container if not exists
    //  <CreateContainerIfNotExists>
    val containerProperties = new CosmosContainerProperties(containerName, "/name")
    //  Create container with 400 RU/s
    val cosmosContainerResponse = database.createContainerIfNotExists(containerProperties, ThroughputProperties.createManualThroughput(400))
    val container = database.getContainer(cosmosContainerResponse.getProperties.getId)
    //  </CreateContainerIfNotExists>
    logger.info("Checking container {} completed!\n", container.getId)
    container
  }

  private def createClothing(container: CosmosContainer): List[Clothing] = {
    val clothingToCreate = List[Clothing](
      new Clothing("1", "shirt", "typical shirt"),
      new Clothing("2", "trousers", "strange trousers"),
      new Clothing("3", "shoes", "funny shoes")
    )

    val totalRequestCharge = clothingToCreate.map(
      clothing => {
        val cosmosItemRequestOptions = new CosmosItemRequestOptions
        val item = container.createItem(clothing, new PartitionKey(clothing.name), cosmosItemRequestOptions)
        //  Get request charge and other properties like latency, and diagnostics strings, etc.
        logger.info("Created item with request charge of {} within duration {}", item.getRequestCharge, item.getDuration)
        item.getRequestCharge
      }
    ).sum
    logger.info("Created {} items with total request charge of {}",
      clothingToCreate.size,
      totalRequestCharge)
    clothingToCreate
  }

  private def readItems(clothingList: List[Clothing], container: CosmosContainer): Unit = {
    clothingList.foreach(clothing => {
      try {
        val item = container.readItem(clothing.getId, new PartitionKey(clothing.getName), classOf[Clothing])
        val requestCharge = item.getRequestCharge
        val requestLatency = item.getDuration
        logger.info("Item successfully read with id {} with a charge of {} and within duration {}", item.getItem.getId, requestCharge, requestLatency)
      } catch {
        case e: CosmosException =>
          logger.error("Read Item failed with", e)
      }
    })
  }


  private def queryItems(container: CosmosContainer): Unit = { //  <QueryItems>
    // Set some common query options
    val queryOptions = new CosmosQueryRequestOptions
    queryOptions.setQueryMetricsEnabled(true)
    val clothingPagedIterable = container.queryItems("SELECT * FROM Clothing WHERE Clothing.name IN ('shirt', 'shoes')", queryOptions, classOf[Clothing])
    clothingPagedIterable.iterableByPage(10).forEach((cosmosItemPropertiesFeedResponse: FeedResponse[Clothing]) => {
      logger.info("Got a page of query result with {} items(s) and request charge of {}", cosmosItemPropertiesFeedResponse.getResults.size, cosmosItemPropertiesFeedResponse.getRequestCharge)
      logger.info("Item Ids {}", cosmosItemPropertiesFeedResponse.getResults.stream.map(i => i.getId).toList)

    })
  }

  def main(args: Array[String]): Unit = {
    var clientOption = None: Option[CosmosClient]
    try {
      logger.info("Hello Azure Cosmos DB")
      val (client, container) = initDB()
      clientOption = Some(client)
      val clothingList = createClothing(container)
      readItems(clothingList, container)
      queryItems(container)
      logger.info("Goodbye")
    } catch {
      case e: Exception =>
        logger.error("CosmosDB failed with", e)
    } finally {
      logger.info("Closing the client")
      clientOption match {
        case Some(client) => client.close()
        case None => logger.warn("Client not created!")
      }
    }
    System.exit(0)
  }
}
