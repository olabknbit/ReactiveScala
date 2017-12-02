package product_catalog_server

//#messages
final case class SearchForItems(words: List[String])
case class SearchResults(catalogSearchResults: CatalogSearchResults)
final case class ProductCatalogJobFailed(reason: String, job: SearchForItems)
case object BackendRegistration
//#messages
