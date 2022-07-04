class Clothing(var id: String, var name: String, var description: String) {
  def this() {
    this("", "", "")
  }

  def getId: String = id

  def setId(id: String): Unit = {
    this.id = id
  }

  def getName: String = name

  def setName(name: String): Unit = {
    this.name = name
  }

  def getDescription: String = description

  def setDescription(description: String): Unit = {
    this.description = description
  }
}
