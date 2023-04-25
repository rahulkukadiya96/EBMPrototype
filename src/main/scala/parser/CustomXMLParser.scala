package parser

import scala.xml.{Elem, SAXParser}
import scala.xml.factory.XMLLoader

object CustomXMLParser extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    f.setNamespaceAware(false)
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    f.newSAXParser()
  }

  def main(args: Array[String]): Unit = {
    import scala.xml.XML

    val xml = """ <Article PubModel="Print-Electronic">
                |              <Journal>
                |                <ISSN IssnType="Electronic">1938-3207</ISSN>
                |                <JournalIssue CitedMedium="Internet">
                |                  <PubDate>
                |                    <Year>2023</Year>
                |                    <Month>Apr</Month>
                |                    <Day>21</Day>
                |                  </PubDate>
                |                </JournalIssue>
                |                <Title>The American journal of clinical nutrition</Title>
                |                <ISOAbbreviation>Am J Clin Nutr</ISOAbbreviation>
                |              </Journal>
                |              <ArticleTitle>Predictors of WIC Uptake Among Low-Income Pregnant Individuals: A Longitudinal Nationwide Analysis.</ArticleTitle>
                |              <ELocationID EIdType="pii" ValidYN="Y">S0002-9165(23)47381-9</ELocationID>
                |              <ELocationID EIdType="doi" ValidYN="Y">10.1016/j.ajcnut.2023.04.023</ELocationID>
                |              <Abstract>
                |                <AbstractText Label="BACKGROUND" NlmCategory="BACKGROUND">Nutrition during pregnancy is important for maternal and infant health. The Special Supplemental Nutrition Program for Women, Infants, and Children (WIC) provides nutritional support for low-income pregnant and postpartum individuals and children under 5. However, WIC participation was in decline in the decade leading up to 2019.</AbstractText>
                |                <AbstractText Label="OBJECTIVE" NlmCategory="OBJECTIVE">This study examined individual and state predictors associated with WIC uptake among eligible individuals, to help identify subgroups for targeted intervention to improve participation.</AbstractText>
                |                <AbstractText Label="METHODS" NlmCategory="METHODS">Data came from the 2004-2019 waves of the Pregnancy Risk Assessment Monitoring System (PRAMS), a national survey of individuals who recently gave birth (N = 288,531). Multivariable logistic regressions were used to examine individual-, state-level, and temporal predictors of WIC uptake among WIC-eligible respondents.</AbstractText>
                |                <AbstractText Label="RESULTS" NlmCategory="RESULTS">Among WIC-eligible respondents, age greater than 35 (OR: 0.68; 95%CI: 0.66, 0.70), more than high school education (OR; 0.63; 95%CI: 062, 0.65), English language (OR: 0.71; 95%CI: 0.68, 0.74), being married (OR: 0.70; 95%CI: 0.69, 0.72), White race, smaller family size, not having pre-pregnancy diabetes, and higher income were associated with lower odds of WIC uptake. Respondents in states with higher Earned Income Tax Credit rates and in the Northeast, Midwest, and West (compared with the South) had lower WIC uptake. Respondents in states with higher gross domestic product, unemployment rates, Supplemental Nutrition Assistance Program, Temporary Assistance for Needy Families, and Medicaid caseloads, and Democrat governors had higher uptake; however, effect estimates were small and may not represent a meaningful change. Associations were strongest during 2009-2015 compared to other years, particularly for race/Hispanic-origin, language, marital status, pre-pregnancy diabetes family size, and pre-pregnancy.</AbstractText>
                |                <AbstractText Label="CONCLUSIONS" NlmCategory="CONCLUSIONS">This study identified several individual and state-level characteristics associated with WIC uptake among low-income eligible respondents, paving the way for future interventions to target key subgroups to improve program participation.</AbstractText>
                |                <CopyrightInformation>Copyright © 2023 The Author(s). Published by Elsevier Inc. All rights reserved.</CopyrightInformation>
                |              </Abstract>
                |              <AuthorList CompleteYN="Y">
                |                <Author ValidYN="Y">
                |                  <LastName>Collin</LastName>
                |                  <ForeName>Daniel F</ForeName>
                |                  <Initials>DF</Initials>
                |                  <AffiliationInfo>
                |                    <Affiliation>Philip R. Lee Institute for Health Policy Studies, University of California San Francisco, San Francisco, California.</Affiliation>
                |                  </AffiliationInfo>
                |                </Author>
                |                <Author ValidYN="Y">
                |                  <LastName>Guan</LastName>
                |                  <ForeName>Alice</ForeName>
                |                  <Initials>A</Initials>
                |                  <AffiliationInfo>
                |                    <Affiliation>Department of Epidemiology &amp; Biostatistics, University of California San Francisco, San Francisco, California.</Affiliation>
                |                  </AffiliationInfo>
                |                </Author>
                |                <Author ValidYN="Y">
                |                  <LastName>Hamad</LastName>
                |                  <ForeName>Rita</ForeName>
                |                  <Initials>R</Initials>
                |                  <AffiliationInfo>
                |                    <Affiliation>Philip R. Lee Institute for Health Policy Studies, University of California San Francisco, San Francisco, California; Department of Family &amp; Community Medicine, University of California San Francisco, San Francisco, California. Electronic address: rita.hamad@ucsf.edu.</Affiliation>
                |                  </AffiliationInfo>
                |                </Author>
                |              </AuthorList>
                |              <Language>eng</Language>
                |              <PublicationTypeList>
                |                <PublicationType UI="D016428">Journal Article</PublicationType>
                |              </PublicationTypeList>
                |              <ArticleDate DateType="Electronic">
                |                <Year>2023</Year>
                |                <Month>04</Month>
                |                <Day>21</Day>
                |              </ArticleDate>
                |            </Article>""".stripMargin

    val xmldata = """ <Abstract>
                    |                <AbstractText Label="BACKGROUND" NlmCategory="BACKGROUND">Nutrition during pregnancy is important for maternal and infant health. The Special Supplemental Nutrition Program for Women, Infants, and Children (WIC) provides nutritional support for low-income pregnant and postpartum individuals and children under 5. However, WIC participation was in decline in the decade leading up to 2019.</AbstractText>
                    |                <AbstractText Label="OBJECTIVE" NlmCategory="OBJECTIVE">This study examined individual and state predictors associated with WIC uptake among eligible individuals, to help identify subgroups for targeted intervention to improve participation.</AbstractText>
                    |                <AbstractText Label="METHODS" NlmCategory="METHODS">Data came from the 2004-2019 waves of the Pregnancy Risk Assessment Monitoring System (PRAMS), a national survey of individuals who recently gave birth (N = 288,531). Multivariable logistic regressions were used to examine individual-, state-level, and temporal predictors of WIC uptake among WIC-eligible respondents.</AbstractText>
                    |                <AbstractText Label="RESULTS" NlmCategory="RESULTS">Among WIC-eligible respondents, age greater than 35 (OR: 0.68; 95%CI: 0.66, 0.70), more than high school education (OR; 0.63; 95%CI: 062, 0.65), English language (OR: 0.71; 95%CI: 0.68, 0.74), being married (OR: 0.70; 95%CI: 0.69, 0.72), White race, smaller family size, not having pre-pregnancy diabetes, and higher income were associated with lower odds of WIC uptake. Respondents in states with higher Earned Income Tax Credit rates and in the Northeast, Midwest, and West (compared with the South) had lower WIC uptake. Respondents in states with higher gross domestic product, unemployment rates, Supplemental Nutrition Assistance Program, Temporary Assistance for Needy Families, and Medicaid caseloads, and Democrat governors had higher uptake; however, effect estimates were small and may not represent a meaningful change. Associations were strongest during 2009-2015 compared to other years, particularly for race/Hispanic-origin, language, marital status, pre-pregnancy diabetes family size, and pre-pregnancy.</AbstractText>
                    |                <AbstractText Label="CONCLUSIONS" NlmCategory="CONCLUSIONS">This study identified several individual and state-level characteristics associated with WIC uptake among low-income eligible respondents, paving the way for future interventions to target key subgroups to improve program participation.</AbstractText>
                    |                <CopyrightInformation>Copyright © 2023 The Author(s). Published by Elsevier Inc. All rights reserved.</CopyrightInformation>
                    |              </Abstract>""".stripMargin


    val final_xml = """ <PubmedArticle>
                      |		<MedlineCitation Status="Publisher" Owner="NLM" IndexingMethod="Automated">
                      |			<PMID Version="1">37088228</PMID>
                      |			<DateRevised>
                      |				<Year>2023</Year>
                      |				<Month>04</Month>
                      |				<Day>23</Day>
                      |			</DateRevised>
                      |			<Article PubModel="Print-Electronic">
                      |				<Journal>
                      |					<ISSN IssnType="Electronic">1938-3207</ISSN>
                      |					<JournalIssue CitedMedium="Internet">
                      |						<PubDate>
                      |							<Year>2023</Year>
                      |							<Month>Apr</Month>
                      |							<Day>21</Day>
                      |						</PubDate>
                      |					</JournalIssue>
                      |					<Title>The American journal of clinical nutrition</Title>
                      |					<ISOAbbreviation>Am J Clin Nutr</ISOAbbreviation>
                      |				</Journal>
                      |				<ArticleTitle>Predictors of WIC Uptake Among Low-Income Pregnant Individuals: A Longitudinal Nationwide Analysis.</ArticleTitle>
                      |				<ELocationID EIdType="pii" ValidYN="Y">S0002-9165(23)47381-9</ELocationID>
                      |				<ELocationID EIdType="doi" ValidYN="Y">10.1016/j.ajcnut.2023.04.023</ELocationID>
                      |				<Abstract>
                      |					<AbstractText Label="BACKGROUND" NlmCategory="BACKGROUND">Nutrition during pregnancy is important for maternal and infant health. The Special Supplemental Nutrition Program for Women, Infants, and Children (WIC) provides nutritional support for low-income pregnant and postpartum individuals and children under 5. However, WIC participation was in decline in the decade leading up to 2019.</AbstractText>
                      |					<AbstractText Label="OBJECTIVE" NlmCategory="OBJECTIVE">This study examined individual and state predictors associated with WIC uptake among eligible individuals, to help identify subgroups for targeted intervention to improve participation.</AbstractText>
                      |					<AbstractText Label="METHODS" NlmCategory="METHODS">Data came from the 2004-2019 waves of the Pregnancy Risk Assessment Monitoring System (PRAMS), a national survey of individuals who recently gave birth (N = 288,531). Multivariable logistic regressions were used to examine individual-, state-level, and temporal predictors of WIC uptake among WIC-eligible respondents.</AbstractText>
                      |					<AbstractText Label="RESULTS" NlmCategory="RESULTS">Among WIC-eligible respondents, age greater than 35 (OR: 0.68; 95%CI: 0.66, 0.70), more than high school education (OR; 0.63; 95%CI: 062, 0.65), English language (OR: 0.71; 95%CI: 0.68, 0.74), being married (OR: 0.70; 95%CI: 0.69, 0.72), White race, smaller family size, not having pre-pregnancy diabetes, and higher income were associated with lower odds of WIC uptake. Respondents in states with higher Earned Income Tax Credit rates and in the Northeast, Midwest, and West (compared with the South) had lower WIC uptake. Respondents in states with higher gross domestic product, unemployment rates, Supplemental Nutrition Assistance Program, Temporary Assistance for Needy Families, and Medicaid caseloads, and Democrat governors had higher uptake; however, effect estimates were small and may not represent a meaningful change. Associations were strongest during 2009-2015 compared to other years, particularly for race/Hispanic-origin, language, marital status, pre-pregnancy diabetes family size, and pre-pregnancy.</AbstractText>
                      |					<AbstractText Label="CONCLUSIONS" NlmCategory="CONCLUSIONS">This study identified several individual and state-level characteristics associated with WIC uptake among low-income eligible respondents, paving the way for future interventions to target key subgroups to improve program participation.</AbstractText>
                      |					<CopyrightInformation>Copyright © 2023 The Author(s). Published by Elsevier Inc. All rights reserved.</CopyrightInformation>
                      |				</Abstract>
                      |				<AuthorList CompleteYN="Y">
                      |					<Author ValidYN="Y">
                      |						<LastName>Collin</LastName>
                      |						<ForeName>Daniel F</ForeName>
                      |						<Initials>DF</Initials>
                      |						<AffiliationInfo>
                      |							<Affiliation>Philip R. Lee Institute for Health Policy Studies, University of California San Francisco, San Francisco, California.</Affiliation>
                      |						</AffiliationInfo>
                      |					</Author>
                      |					<Author ValidYN="Y">
                      |						<LastName>Guan</LastName>
                      |						<ForeName>Alice</ForeName>
                      |						<Initials>A</Initials>
                      |						<AffiliationInfo>
                      |							<Affiliation>Department of Epidemiology &amp; Biostatistics, University of California San Francisco, San Francisco, California.</Affiliation>
                      |						</AffiliationInfo>
                      |					</Author>
                      |					<Author ValidYN="Y">
                      |						<LastName>Hamad</LastName>
                      |						<ForeName>Rita</ForeName>
                      |						<Initials>R</Initials>
                      |						<AffiliationInfo>
                      |							<Affiliation>Philip R. Lee Institute for Health Policy Studies, University of California San Francisco, San Francisco, California; Department of Family &amp; Community Medicine, University of California San Francisco, San Francisco, California. Electronic address: rita.hamad@ucsf.edu.</Affiliation>
                      |						</AffiliationInfo>
                      |					</Author>
                      |				</AuthorList>
                      |				<Language>eng</Language>
                      |				<PublicationTypeList>
                      |					<PublicationType UI="D016428">Journal Article</PublicationType>
                      |				</PublicationTypeList>
                      |				<ArticleDate DateType="Electronic">
                      |					<Year>2023</Year>
                      |					<Month>04</Month>
                      |					<Day>21</Day>
                      |				</ArticleDate>
                      |			</Article>
                      |			<MedlineJournalInfo>
                      |				<Country>United States</Country>
                      |				<MedlineTA>Am J Clin Nutr</MedlineTA>
                      |				<NlmUniqueID>0376027</NlmUniqueID>
                      |				<ISSNLinking>0002-9165</ISSNLinking>
                      |			</MedlineJournalInfo>
                      |			<CitationSubset>IM</CitationSubset>
                      |			<KeywordList Owner="NOTNLM">
                      |				<Keyword MajorTopicYN="N">Nutrition</Keyword>
                      |				<Keyword MajorTopicYN="N">PRAMS</Keyword>
                      |				<Keyword MajorTopicYN="N">WIC uptake</Keyword>
                      |				<Keyword MajorTopicYN="N">health disparities</Keyword>
                      |				<Keyword MajorTopicYN="N">pregnancy</Keyword>
                      |			</KeywordList>
                      |		</MedlineCitation>
                      |		<PubmedData>
                      |			<History>
                      |				<PubMedPubDate PubStatus="received">
                      |					<Year>2022</Year>
                      |					<Month>10</Month>
                      |					<Day>21</Day>
                      |				</PubMedPubDate>
                      |				<PubMedPubDate PubStatus="revised">
                      |					<Year>2023</Year>
                      |					<Month>4</Month>
                      |					<Day>14</Day>
                      |				</PubMedPubDate>
                      |				<PubMedPubDate PubStatus="accepted">
                      |					<Year>2023</Year>
                      |					<Month>4</Month>
                      |					<Day>19</Day>
                      |				</PubMedPubDate>
                      |				<PubMedPubDate PubStatus="medline">
                      |					<Year>2023</Year>
                      |					<Month>4</Month>
                      |					<Day>24</Day>
                      |					<Hour>0</Hour>
                      |					<Minute>41</Minute>
                      |				</PubMedPubDate>
                      |				<PubMedPubDate PubStatus="pubmed">
                      |					<Year>2023</Year>
                      |					<Month>4</Month>
                      |					<Day>24</Day>
                      |					<Hour>0</Hour>
                      |					<Minute>41</Minute>
                      |				</PubMedPubDate>
                      |			</History>
                      |			<PublicationStatus>aheadofprint</PublicationStatus>
                      |			<ArticleIdList>
                      |				<ArticleId IdType="pubmed">37088228</ArticleId>
                      |				<ArticleId IdType="doi">10.1016/j.ajcnut.2023.04.023</ArticleId>
                      |				<ArticleId IdType="pii">S0002-9165(23)47381-9</ArticleId>
                      |			</ArticleIdList>
                      |		</PubmedData>
                      |	</PubmedArticle>""".stripMargin
    val abstractTexts = (XML.loadString(final_xml) \ "MedlineCitation" \ "Article" \"Abstract" \ "AbstractText")
      .map(e => (e \@ "Label", e.text))
      .toMap

    abstractTexts.foreach { case (label, text) =>
      println(s"$label: $text")
    }

  }
}
