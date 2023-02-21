# EBM Prototype (Evidence based medicine)

This is the basic prototype for the evidence based medicine concept. 
It is being developed by Rahul Kukadiya under the supervision of Sabah Mohammed and Jinan Fiaidhi.

# Best Practice
    1. Introduce the fetchers for the database query. 

## Parse User Defined object in graphQL Mutation or Query
    1. Define the deriveInputObjectType :
        lazy val CCEncInputType: InputObjectType[CCEncounter] = deriveInputObjectType[CCEncounter] (
            InputObjectTypeName("CC_ENC_INPUT TYPE")
        )
    2. Create Formattter so that Spray-json can marshall and unmarshall object in json
        implicit val ccEncFormat: RootJsonFormat[CCEncounter] = jsonFormat4(CCEncounter)
        private val CCEncArg = Argument("ccEnc", CCEncInputType)
    3. Define the Argument
        private val CCEncArg = Argument("ccEnc", CCEncInputType)

# Resources
1) https://index.scala-lang.org/bmc/grizzled-slf4j/artifacts/grizzled-slf4j
2) https://devlms.com/graphql-scala/8-mutations/
3) https://stackoverflow.com/questions/25178108/converting-datetime-to-a-json-string