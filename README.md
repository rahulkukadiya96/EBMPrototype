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

## EBM Workflow 
SOAP Notes --> PICO Elements --> Mesh Elements (Search Terms) --> Build Search Strategy --> Fetch Result --> Add filter (Language, Published date) to narrow down the result

# PICO Elements
1) Patient or Problem
   It refers to a "S" (Subjective) section of the SOAP. It could be symptom, diagnosis, or concern that the patient has reported. i.e. obese patients
2) Intervention 
   It refers to a "O" (Objective) section of the SOAP. It could be medication, procedure, or other therapeutic measure. i.e. chitosan
3) Comparison
   If applicable, describe any alternative interventions or treatments being considered. i.e. placebo
4) Outcome
   Identify the desired outcome or the goal of the intervention. i.e. decrease weight

For example, if a SOAP note describes a patient with a cough and fever who was diagnosed with pneumonia and started on antibiotics 
The PICO elements could be:
* P: Patients with pneumonia symptoms
* I: Antibiotic treatment
* C: Alternative antibiotic treatment options
* O: Improvement in pneumonia symptoms

# Possible Question Sets

## Problem or Patient
*   What is the age range of the patients? 
*   What is the gender distribution of the patients?
*   What is the ethnicity or race of the patients?
*   What is the socioeconomic status of the patients?
*   What is the medical history of the patients?
*   What is the current health status of the patients?
*   Are there any comorbidity present in the patients?
*   What is the geographic location of the patients?
*   Are there any exclusion criteria for the patients?

## Intervention or Exposure
* What is the treatment being considered?
* What is the dose of the treatment?
* What is the duration of the treatment?
* What is the mode of delivery of the treatment?
* Are there any alternative treatments available?

## Comparison Intervention or Exposure
* What is the standard treatment for this condition?
* What is the placebo treatment for this condition?
* What is the comparator treatment for this condition?

## Outcome of Interest
* What is the primary outcome of interest?
* What is the secondary outcome of interest?
* What is the timeframe for measuring outcomes?
* What is the method for measuring outcomes?

## Time Frame or Setting
* What is the timeframe for the study?
* Is this a retrospective or prospective study?
* What is the follow-up period?
* What is the time period for data collection?


# Search Terms
**Keywords** 
    Normal words that are coming to you naturally. 
    Searching by keyword finds only those results where your keyword appears as an exact match in several fields including the title or abstract. 
    This works particularly well if you are looking for a specific spelling, product, term, or phrase. i.e. bushfire. 
    This is required when we have less subject terms. 
    It will give too much irrelevant information

**Subject headings** 
    These terms that have been identified and defined to cover a particular concept. 
    We used mesh database to find the alternatives and use this alternative to search the data. i.e. heart attack. 
    This is give more relevant information.
    Synonyms are included under broad subject headings in sophisticated databases such as MEDLINE, Embase, or PsycINFO. Word spellings may also vary: ‘paediatrics’ in Australia is expressed as ‘pediatrics’ in the USA.

https://www.ciap.health.nsw.gov.au/training/ebp-learning-modules/module2/identifying-search-terms-keywords-and-subject-headings.html


# Build Search Strategy
Below operators is used to fetch the result from the Pubmed database
1) Or :- Find the result which contains any or the terms which is mentioned in the query. The elements from the same section fallen with or condition. 
2) And :- Find the result which contains both the terms or query which is mentioned in the query. The elements from the different section fallen with and condition.
3) Asterisk * :- This is used to deal with spelling variation of same word. i.e. Adolescent or Adolescences or Adolescence would cover with Adolescen*
So, Ideally query would be (P1 OR P2 OR P3) and (I1 or I2) and (C1 or C2) and (O1 or O2 or O3)

* **Scenario**: A 64-year-old obese male who has tried many ways to lose weight presents with a newspaper article about ‘fat-blazer’ (chitosan). He asks for your advice.
  * Population/problem ::-	obes* OR overweight (2)
  * Indicator  ::- (intervention, test, etc)	chitosan (1)
  *  Comparator ::-	placebo (4)
  *  Outcome	::- decrease weight OR kilogram* (3) 
  
   Query would be **#1 AND #2 AND #3 AND #4**

# Query Filter
This is used to narrow down the results. It includes some basic filter like publication date, species article type etc.

# Database Configuration
1) Neo4J : A Graph database to store the SOAP. I used the 3.5 community edition which is worked on the JDK 8.
2) Basic commands are, 
   * Install Neo4J Service (neo4j install-service)
   * Start the Service (neo4j start)
   * Uninstall the Service (neo4j uninstall-service)
   * Update the Service (neo4j update-service)


# Resources
1) https://index.scala-lang.org/bmc/grizzled-slf4j/artifacts/grizzled-slf4j
2) https://devlms.com/graphql-scala/8-mutations/
3) https://stackoverflow.com/questions/25178108/converting-datetime-to-a-json-string
4) https://www.cebm.ox.ac.uk/resources/ebm-tools/finding-the-evidence-tutorial
5) https://www.ciap.health.nsw.gov.au/training/ebp-learning-modules/
6) https://neo4j.com/docs/operations-manual/3.5/installation/windows/