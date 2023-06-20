package ma.eai.transverse.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.itextpdf.text.*;
import com.itextpdf.text.List;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import ma.eai.transverse.model.ReturnOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import eu.europa.esig.dss.validation.reports.Reports;
import lombok.NoArgsConstructor;
import ma.eai.transverse.model.PdfGenerator;
import ma.eai.transverse.repositories.GeneratePdfRepository;

@Service
@Transactional
@NoArgsConstructor
@CrossOrigin("*")
public class GenerateReportService {

	@Autowired
	private GeneratePdfRepository generatePdfRepository;
	String currentDateGenerationReport;
	ByteArrayOutputStream generatedReportByte;

	Font fontLoi = FontFactory.getFont("Candara", 8, Font.ITALIC);
	Font fontNineSimple = FontFactory.getFont("Candara", 9);
	Font fontNineBold = FontFactory.getFont("Candara", 9, Font.BOLD);
	Font fontNineBoldBlue = FontFactory.getFont("Candara", 9, Font.BOLD, new BaseColor(47, 84, 150));
	//Font font = FontFactory.getFont("Candara", 14, Font.BOLD, new BaseColor(47, 84, 150));
	Font fontTenSimple = FontFactory.getFont("Candara", 10);
	Font fontErr = FontFactory.getFont("Candara", 10, BaseColor.RED);
	Font fontTenBold = FontFactory.getFont("Candara", 10, Font.BOLD);
    Font fontTenBoldBlue = FontFactory.getFont("Candara", 10, Font.BOLD, new BaseColor(46, 116, 181));
	Font fontTenSimpleBlue = FontFactory.getFont("Candara", 10, new BaseColor(46, 116, 181));

	Font fontConclBody = FontFactory.getFont("Candara", 11, Font.ITALIC);

    @SuppressWarnings("unused")
    public void createCell(String descsription, String separator, String content, PdfPTable tableToAdd, Font fontTitle, Font fontContent){
        PdfPCell date1 = new PdfPCell(new Phrase(descsription, fontTitle));
        date1.setBorder(Rectangle.NO_BORDER);
        tableToAdd.addCell(date1);

        PdfPCell date2 = new PdfPCell(new Phrase(separator, fontTitle));
        date2.setBorder(Rectangle.NO_BORDER);
        tableToAdd.addCell(date2);

        PdfPCell date3 = new PdfPCell(new Phrase(content, fontContent));
        date3.setBorder(Rectangle.NO_BORDER);
        tableToAdd.addCell(date3);
    }

    public java.util.List<Phrase> singlePhrase(java.util.List<java.util.List<Object>> listOfPhrases){
        java.util.List<Phrase> listPhrases = new ArrayList<>();

        for(int i=0; i<listOfPhrases.size();i++){
            Phrase phraseCn = new Phrase();
            Chunk chunk1Cn = new Chunk((String) listOfPhrases.get(i).get(0) , (Font) listOfPhrases.get(i).get(1));
            Chunk chunk2Cn = new Chunk(listOfPhrases.get(i).get(2).toString(), (Font) listOfPhrases.get(i).get(3));
            phraseCn.add(chunk1Cn);
            phraseCn.add(" ");
            phraseCn.add(chunk2Cn);

            listPhrases.add(phraseCn);
        }

        return listPhrases;
    }
    public PdfPCell createList(String listSymbol, String titreList, Font fontTitreList, java.util.List<Phrase> listePhrase){
        PdfPCell cellSignataire = new PdfPCell();
        cellSignataire.setBorder(Rectangle.BOX);
		cellSignataire.setPadding(5f);

        List parentList = new List(List.UNORDERED);
        parentList.setListSymbol(new Chunk(listSymbol));
        parentList.add(new ListItem(titreList, fontTitreList));

        List subList = new List(List.UNORDERED);
        subList.setIndentationLeft(20);
        subList.setListSymbol(new Chunk(""));
        for(Phrase phrase:listePhrase){
            subList.add(new ListItem(phrase));
        }
        subList.setListSymbol(new Chunk(""));
        parentList.add(subList);
        cellSignataire.addElement(parentList);

        return cellSignataire;
    }
	public PdfPTable checkControleReglementaire(String condition, Boolean regle,PdfPTable tableConditions) throws BadElementException, IOException {
		java.util.List<String> prestaQualifie = Arrays.asList("Eurafric Information","Barid Al Maghrib");

		PdfPCell row1Col1 = new PdfPCell(new Phrase(condition,fontTenSimple));
		row1Col1.setPadding(5f);
		tableConditions.addCell(row1Col1);

		PdfPCell row1Col2 = new PdfPCell();
		row1Col2.setPadding(5f);
		row1Col2.setHorizontalAlignment(Element.ALIGN_CENTER);
		row1Col2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(regle){
			Image checkImage = Image.getInstance(getClass().getResource("/images/true.png"));
			checkImage.scaleToFit(15f, 15f);
			checkImage.setAlignment(Image.ALIGN_CENTER);
			row1Col2.addElement(checkImage);
		} else {
			Image crossMark = Image.getInstance(getClass().getResource("/images/false.png"));
			crossMark.scaleToFit(15f, 15f);
			crossMark.setAlignment(Image.ALIGN_CENTER);
			row1Col2.addElement(crossMark);
		}
		tableConditions.addCell(row1Col2);

		return tableConditions;
	}

	public PdfPTable dblRow(String titre, Font fontTitre, String contenu, Font fontContenu){
		PdfPTable tableIdSign = new PdfPTable(1);
		tableIdSign.setWidthPercentage(100);
		if(titre.equals("Résultat du contrôle :\n")){
			tableIdSign.setSpacingBefore(11f);
		}
		tableIdSign.setSpacingAfter(11f);
		tableIdSign.getDefaultCell().setBorder(1);

		Chunk IdSign = new Chunk(titre, fontTitre);
		Chunk detIdSign = new Chunk(contenu, fontContenu);

		Paragraph paragraphIdSign = new Paragraph();
		paragraphIdSign.add(IdSign);
		paragraphIdSign.add(detIdSign);

		PdfPCell cellIdSign = new PdfPCell();
		cellIdSign.addElement(paragraphIdSign);
		cellIdSign.setPaddingBottom(10f);
		cellIdSign.setPaddingLeft(4f);

		tableIdSign.addCell(cellIdSign);

		return tableIdSign;
	}

	public PdfGenerator export(Reports reports, MultipartFile file) throws IOException, DocumentException, NoSuchAlgorithmException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy 'à' HH:mm:ss 'GMT'XXX");
		currentDateGenerationReport = simpleDateFormat.format(new Date());

		Document doc = new Document(PageSize.A4);

		generatedReportByte = new ByteArrayOutputStream();

		PdfWriter.getInstance(doc, generatedReportByte);

		doc.open();

		/*debut logo*/
		PdfPTable table = new PdfPTable(1);
		table.setWidthPercentage(14);
		table.getDefaultCell().setBorder(0);
		table.setHorizontalAlignment(Element.ALIGN_LEFT);

		Image logo = Image.getInstance(getClass().getResource("/images/AfricTrustLogo.png"));
		logo.scaleToFit(5, 2);
		PdfPCell cell = new PdfPCell(logo, true);
		cell.setBorder(0);
		table.addCell(cell);

		doc.add(table);
		/*fin logo*/

		/*debut titre : rapportde validation...*/
		BaseColor backgroundColor = new BaseColor(242, 242, 242);
		PdfPTable titre = new PdfPTable(1);
		titre.setWidthPercentage(100);
		titre.getDefaultCell().setBorder(Rectangle.TOP | Rectangle.BOTTOM);
		titre.setSpacingBefore(10f);
		titre.setSpacingAfter(10f);

		Paragraph paragraph = new Paragraph("RAPPORT DE VALIDATION \n DES SERVICES DE CONFIANCE QUALIFIES");
		paragraph.setFont(fontNineBoldBlue);
		paragraph.setAlignment(Paragraph.ALIGN_CENTER);
		paragraph.setSpacingAfter(5f);

		PdfPCell cellTitre = new PdfPCell();
		cellTitre.setBackgroundColor(backgroundColor);
		cellTitre.addElement(paragraph);
		cellTitre.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cellTitre.setHorizontalAlignment(Element.ALIGN_CENTER);
		cellTitre.setBorder(Rectangle.TOP | Rectangle.BOTTOM);

		titre.addCell(cellTitre);
		doc.add(titre);
		/*fin titre : rapportde validation...*/

		/*debut loi*/
		PdfPTable tablee = new PdfPTable(1);
		tablee.setWidthPercentage(100);
		tablee.getDefaultCell().setBorder(0);
		doc.add(tablee);

//		Font fontLoi = FontFactory.getFont("Candara", 8, Font.ITALIC);
		//fontLoi.setSize(8);

		List list = new List();
		list.setListSymbol(new Chunk("\u2022"));
		list.setIndentationLeft(15f);
		list.add(new ListItem("\u0020\u0020Rapport généré via la plateforme Afric’ChekSIGN du tiers de confiance AfricTRUST homologué par l’autorité nationale (Direction Générale de la Sécurité des Systèmes d’Information) en qualité de PSCE (Prestataire de services de certification électronique)", fontLoi));
		ListItem secondItem = new ListItem("\u0020\u0020Le contrôle de validité respecte les exigences de la loi marocaine 43-02 relative aux services de confiance pour les transactions électroniques :", fontLoi);

		// Create two sub-lists for the second item
		List subList1 = new List();
		subList1.setListSymbol(new Chunk("\u00BB"));
		subList1.setIndentationLeft(30f);
		subList1.add(new ListItem("\u0020\u0020Signature électronique qualifiée : Article 10 & 11", fontLoi));
		subList1.setIndentationLeft(20f);
		subList1.add(new ListItem("\u0020\u0020Cachet-électronique qualifié : Article 19 & 20", fontLoi));
		// Add the sub-lists to the second item
		secondItem.add(subList1);
		// Add the second item to the main list
		list.add(secondItem);

		list.getLastItem().setSpacingAfter(10f);
		doc.add(list);
		/*fin loi*/

		/*debut synthese*/
		Chunk chunk = new Chunk("SYNTHESE GLOBALE", fontNineBoldBlue);
		doc.add(chunk);
		// Add a line below the title
		Paragraph line = new Paragraph(new Chunk(new LineSeparator()));
		line.setSpacingBefore(-12f);
		doc.add(line);

		/*content*/
		PdfPTable tableSynthese = new PdfPTable(3);
		tableSynthese.setWidthPercentage(100);
		float[] columnWidths = {27f, 2f, 71f};
		tableSynthese.setWidths(columnWidths);
		tableSynthese.getDefaultCell().setBorder(Rectangle.NO_BORDER);

		// Add data
		//date de generation
        createCell("Date de génération",":",currentDateGenerationReport,tableSynthese, fontNineBold, fontNineSimple);
		//nom document
        String nomDocVerifie = reports.getDiagnosticData().getDocumentName();
        createCell("Nom du document vérifié",":",nomDocVerifie,tableSynthese, fontNineBold, fontNineSimple);
		//taille de document en ko
        createCell("Taille",":",Operations.getTailleDocument(file),tableSynthese, fontNineBold, fontNineSimple);
		//empreinte de document
        createCell("Empreinte du document",":",Operations.getEmpreintDocument(file),tableSynthese, fontNineBold, fontNineSimple);
		//Nbr signature
		Set<SignatureWrapper> signCach = reports.getDiagnosticData().getAllSignatures();
		Map<String,Integer> numeroSignatureCachet = Operations.getNumerSignatureCachet(signCach);
        createCell("Nbr. Signatures électroniques",":",String.valueOf(numeroSignatureCachet.get("Signature") != 0 ? numeroSignatureCachet.get("Signature") : "Aucune signature n’est détecté"),tableSynthese, fontNineBold, fontNineSimple);
		//Nbr cachet
        createCell("Nbr. Cachet électronique",":",String.valueOf(numeroSignatureCachet.get("Cachet") != 0 ? numeroSignatureCachet.get("Cachet") : "Aucun cachet n’est détecté"),tableSynthese, fontNineBold, fontNineSimple);
		//Signature / Cachet Inconnue
        createCell("Nbr. Signature ou Cachet électronique Inconnue",":",String.valueOf(numeroSignatureCachet.get("Inconnue")),tableSynthese, fontNineBold, fontNineSimple);

		// Add a spacing of 20 units in the top of the table
		tableSynthese.setSpacingBefore(10);
		tableSynthese.setSpacingAfter(11f);
		// Add the table to the document
		doc.add(tableSynthese);
		/*fin synthese*/

		/*debut resultat controle*/
		Set<SignatureWrapper> allSignatures = reports.getDiagnosticData().getAllSignatures();
		Iterator<SignatureWrapper> iterator = allSignatures.iterator();

		Map<java.util.List<ReturnOperation>,String> returnedOperationsWithUsagePrevu = Operations.MakeOperations(iterator);

		java.util.List<ReturnOperation> listeReturnOperation=null;
		String usagePrevu=null;
		for (Map.Entry<java.util.List<ReturnOperation>, String> entry : returnedOperationsWithUsagePrevu.entrySet()) {
			listeReturnOperation = entry.getKey();
			usagePrevu = entry.getValue();
		}
		doc.add(
				dblRow("Résultat du contrôle :\n", fontTenBold, usagePrevu, fontNineBold)
		);
		/*fin resultat controle*/

		/*start controle Detaille*/
		Chunk chunkContDetail = new Chunk("CONTRÔLE DETAILLE DES SIGNATURES ELECTRONIQUES", fontNineBoldBlue);
		doc.add(chunkContDetail);

		// Add a line below the title
		Paragraph lineContDetail = new Paragraph(new Chunk(new LineSeparator()));
		lineContDetail.setSpacingBefore(-12f);
		doc.add(lineContDetail);

		String niveau = null;
		java.util.List<String> erreursSingleSign = new ArrayList<>();
        int idSignature = 0;

		for(ReturnOperation singleOperation:listeReturnOperation){
			idSignature++;

			PdfPTable Sign = new PdfPTable(1);
			Sign.setWidthPercentage(100);
			Sign.getDefaultCell().setBorder(0);
			Sign.setSpacingBefore(5f);
			Sign.setSpacingAfter(10f);

			Paragraph paragSign = new Paragraph("SIGNATURE : # " + idSignature);
			paragSign.setFont(fontTenBold);

			PdfPCell cellSignCont = new PdfPCell();
			cellSignCont.setBackgroundColor(backgroundColor);
			cellSignCont.addElement(paragSign);
			cellSignCont.setBorder(0);
			cellSignCont.setPaddingBottom(9f);

			Sign.addCell(cellSignCont);
			doc.add(Sign);

			//Information de la signature
			Chunk chunkInfoSign = new Chunk("Informations de la signature :", fontTenBoldBlue);
			doc.add(chunkInfoSign);

			//erreurs:
			doc.add(dblRow("Erreurs :\n",fontTenBold
							,(singleOperation.getErreursSingleSign()!=null && singleOperation.getErreursSingleSign().size()!=0) ? String.join("\n", singleOperation.getErreursSingleSign()) :"Aucun erreur"
							,fontErr)
			);

			//id signature
			doc.add(dblRow("Identifiant de la signature : \n", fontTenBold,singleOperation.getSignatureId(), fontNineSimple));

			//signataire:
			PdfPTable tableSignataire = new PdfPTable(1);
			tableSignataire.setWidthPercentage(100);
			//tableSignataire.setSpacingBefore(11f);
			tableSignataire.getDefaultCell().setBorder(1);

            java.util.List<Object> phrase1 = Arrays.asList("UID : ", fontTenSimpleBlue, singleOperation.getResultList().get("UID")!=null ? singleOperation.getResultList().get("UID") : "----", fontTenSimple);
            java.util.List<Object> phrase2 = Arrays.asList("Nom commun : ",fontTenSimpleBlue,singleOperation.getResultList().get("CN")!=null ? singleOperation.getResultList().get("CN") : "----", fontTenSimple);
            java.util.List<Object> phrase3 = Arrays.asList("Code Pays : ",fontTenSimpleBlue,singleOperation.getResultList().get("C")!=null ? singleOperation.getResultList().get("C") : "----", fontTenSimple);
            java.util.List<java.util.List<Object>> allPhrase = Arrays.asList(phrase1,phrase2,phrase3);

			tableSignataire.addCell(
					createList("", "Signataire :", fontTenBold,
							singlePhrase(allPhrase)));
			doc.add(tableSignataire);

			//PSCo
			PdfPTable tablePSCo = new PdfPTable(1);
			tablePSCo.setWidthPercentage(100);
			tablePSCo.setSpacingBefore(11f);
			tablePSCo.getDefaultCell().setBorder(1);

			java.util.List<Object> phrase4 = Arrays.asList("Autorité de certification : ", fontTenSimpleBlue, singleOperation.getResultlistpso().get("CN")!=null ? singleOperation.getResultlistpso().get("CN") : "----", fontTenSimple);
			java.util.List<Object> phrase5 = Arrays.asList("Identifiant de l’organisation : ", fontTenSimpleBlue, singleOperation.getResultlistpso().get("2.5.4.97") == null ? "--------" : singleOperation.getIdOrganizationHezadecimal(), fontTenSimple);
			java.util.List<Object> phrase6 = Arrays.asList("Nom de l'unité organisationnelle : ", fontTenSimpleBlue, singleOperation.getResultlistpso().get("C")!=null ? singleOperation.getResultlistpso().get("C") : "----", fontTenSimple);
			java.util.List<Object> phrase7 = Arrays.asList("Code pays : ", fontTenSimpleBlue, singleOperation.getResultlistpso().get("C")!=null ? singleOperation.getResultlistpso().get("C") : "----", fontTenSimple);
			java.util.List<java.util.List<Object>> allPhrase1 = Arrays.asList(phrase4,phrase5,phrase6, phrase7);

			tablePSCo.addCell(
					createList("", "PSCo émetteur du certificat de signature :", fontTenBold,
							singlePhrase(allPhrase1)));
			tablePSCo.setSpacingAfter(15f);
			doc.add(tablePSCo);

			//Propriétés de la signature :
			//titre:
			Chunk chunkpropSign = new Chunk("Propriétés de la signature :", fontTenBoldBlue);
			doc.add(chunkpropSign);

			//body
			PdfPTable tablepropSign = new PdfPTable(1);
			tablepropSign.setWidthPercentage(100);
			tablepropSign.setSpacingAfter(18f);
			tablepropSign.getDefaultCell().setBorder(1);

			java.util.List<Object> phrase8 = Arrays.asList("Statut de la signature : ", fontTenSimpleBlue, "************************", fontTenSimple);
			java.util.List<Object> phrase9 = Arrays.asList("Date et heure indiquée : ", fontTenSimpleBlue, simpleDateFormat.format(singleOperation.getDateSignature()), fontTenSimple);
			java.util.List<Object> phrase10 = Arrays.asList("Algorithme de signature : ", fontTenSimpleBlue, singleOperation.getAlgoSignStringWithHash(), fontTenSimple);
			java.util.List<Object> phrase11 = Arrays.asList("Longeur de cle de hashage : ", fontTenSimpleBlue, singleOperation.getLongeurAlgoSign(), fontTenSimple);
			java.util.List<Object> phrase12 = Arrays.asList("Format : ", fontTenSimpleBlue, "PAdES", fontTenSimple);
			java.util.List<Object> phrase13;
			if(singleOperation.isQcType() || singleOperation.isQcComplianc()) {
				niveau = "Qualifiée";
				phrase13 = Arrays.asList("Niveau : ", fontTenSimpleBlue, niveau, fontTenSimple);
			}else{
				niveau = "Non Qualifiée";
				phrase13 = Arrays.asList("Niveau : ", fontTenSimpleBlue, niveau, fontErr);
			}
			java.util.List<Object> phrase14 = Arrays.asList("Intégrité : ", fontTenSimpleBlue, singleOperation.getIntegriteRes(), singleOperation.getModifValid().contains(false) ? fontErr : fontTenSimple);
			java.util.List<java.util.List<Object>> allPhrase2 = Arrays.asList(phrase8,phrase9,phrase10,phrase11,phrase12,phrase13,phrase14);

			tablepropSign.addCell(
					createList("", "", fontTenBold,
							singlePhrase(allPhrase2)));
			doc.add(tablepropSign);

			//Certificat de la signature :
			//titre:
			Chunk chunkCertSign = new Chunk("Certificat du signataire :", fontTenBoldBlue);
			doc.add(chunkCertSign);

			//body
			PdfPTable tableCertSign = new PdfPTable(1);
			tableCertSign.setWidthPercentage(100);
			tableCertSign.setSpacingAfter(18f);
			tableCertSign.getDefaultCell().setBorder(1);

			java.util.List<Object> phrase15 = Arrays.asList("Numéro de série : ", fontTenSimpleBlue, singleOperation.getSerialNumberHex().toUpperCase(), fontTenSimple);
			java.util.List<Object> phrase16 = Arrays.asList("Usages prévus  : ", fontTenSimpleBlue, singleOperation.getUsagPrevuString(), fontTenSimple);
			java.util.List<Object> phrase17 = Arrays.asList("Période de validité : ", fontTenSimpleBlue, "Du  " + simpleDateFormat.format(singleOperation.getDebutValidCert())  + "  Au  " + simpleDateFormat.format(singleOperation.getFinValidCert()), fontTenSimple);

			java.util.List<Object> phrase18;
			boolean dateCertValid;
			if (singleOperation.getDateSignature().after(singleOperation.getDebutValidCert()) && singleOperation.getDateSignature().before(singleOperation.getFinValidCert())) {
				dateCertValid=true;
				phrase18 = Arrays.asList("Certificat utilisé dans sa période de validité ? : ", fontTenSimpleBlue, "OUI", fontTenSimple);
			} else {
				dateCertValid=false;
				phrase18 = Arrays.asList("Certificat utilisé dans sa période de validité ? : ", fontTenSimpleBlue, "NON",fontErr);
			}

			java.util.List<Object> phrase19 = Arrays.asList("Certificat électronique qualifié ? : ", fontTenSimpleBlue, singleOperation.isQcType() ? "OUI" : "NON",singleOperation.isQcType() ? fontTenSimple : fontErr);
			java.util.List<Object> phrase20 = Arrays.asList("Support de signature électronique qualifié ? : ", fontTenSimpleBlue,singleOperation.isQscsc() ? "OUI" : "NON",singleOperation.isQscsc() ? fontTenSimple : fontErr);
			java.util.List<Object> phrase21 = Arrays.asList("Statut de révocation du certificat : ", fontTenSimpleBlue,singleOperation.isRevoked()==false ? "Valide" : "Non valide",singleOperation.isRevoked()==false ? fontTenSimple : fontTenSimpleBlue);

			java.util.List<Object> phrase22;
			if (singleOperation.isCheckValid() && singleOperation.isCheckIntact() && singleOperation.isRootCertificate()) {
				phrase22 = Arrays.asList("Chaine de certification validée ? : ", fontTenSimpleBlue, "OUI", fontTenSimple);
			} else {
				phrase22 = Arrays.asList("Chaine de certification validée ? : ", fontTenSimpleBlue, "NON", fontTenSimple);
			}

			java.util.List<java.util.List<Object>> allPhrase3 = Arrays.asList(phrase15,phrase16,phrase17,phrase18,phrase19,phrase20,phrase21,phrase22);

			tableCertSign.addCell(
					createList("", "", fontTenBold,
							singlePhrase(allPhrase3)));
			doc.add(tableCertSign);

			//Contrôle réglementaire conformément aux exigences de la loi 43-20 :
			//titre:
			Chunk chunkContReg = new Chunk("Contrôle réglementaire conformément aux exigences de la loi 43-20 :",fontTenBoldBlue);
			doc.add(chunkContReg);

			PdfPTable tableConditions = new PdfPTable(2);
			tableConditions.setWidthPercentage(100);
			tableConditions.getDefaultCell().setBorder(1);
			float[] columnWidthsCond = {85f, 15f};
			tableConditions.setWidths(columnWidthsCond);

			// Creating header row
			PdfPCell header1 = new PdfPCell(new Phrase("CONDITIONS", FontFactory.getFont("Candara", 11, Font.BOLD, BaseColor.BLACK)));
			header1.setBackgroundColor(new BaseColor(242, 242, 242));
			header1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header1.setPadding(5f);
			tableConditions.addCell(header1);

			PdfPCell header2 = new PdfPCell(new Phrase("STATUT", FontFactory.getFont("Candara", 11, Font.BOLD, BaseColor.BLACK)));
			header2.setBackgroundColor(new BaseColor(242, 242, 242));
			header2.setHorizontalAlignment(Element.ALIGN_CENTER);
			header2.setVerticalAlignment(Element.ALIGN_MIDDLE);
			header2.setPadding(5f);
			tableConditions.addCell(header2);

			// Creating data rows
			//cell1
			java.util.List<String> prestaQualifie = new ArrayList<>();
			prestaQualifie.add("Eurafric Information");
			prestaQualifie.add("Barid Al Maghrib");

			checkControleReglementaire(
					"Le certificat sur lequel repose la signature ait été, au moment de la signature, un certificat qualifié de signature électronique conformément aux dispositions de l’article 9"
					,singleOperation.isQcType() && singleOperation.isQscsc() && singleOperation.getUsagPrevuListElem().contains("NON_REPUDIATION") && ( singleOperation.getResultlistpso().get("O").equals(prestaQualifie.get(0)) || singleOperation.getResultlistpso().get("O").equals(prestaQualifie.get(1)) )
					,tableConditions);
			//cell2
			checkControleReglementaire(
					"Le certificat qualifié ait été délivré par un prestataire de services de confiance agréé et était valide au moment de la signature"
					,!singleOperation.isRevoked() && dateCertValid
					,tableConditions
			);
			//cell 3
			checkControleReglementaire(
					"Les données de validation de la signature correspondent aux données communiquées à la partie utilisatrice "
					,true
					,tableConditions
			);
			//cell 4
			checkControleReglementaire(
					"L’ensemble unique de données représentant le signataire dans le certificat soit correctement fourni à la partie utilisatrice "
					,(singleOperation.getMail1()!=null || singleOperation.getMail2()!=null) && singleOperation.getResultList().get("C")!=null && singleOperation.getResultList().get("UID")!=null
					,tableConditions
			);
			//cell 5
			checkControleReglementaire(
					"L’utilisation d’un pseudonyme soit clairement indiquée à la partie utilisatrice, si un pseudonyme a été utilisé au moment de la signature ;"
					,true
					,tableConditions
			);
			//cell 6
			checkControleReglementaire(
					"La signature électronique ait été créée par un dispositif qualifié de création de signature électronique et les conditions prévues à l’article 5 de la loi 43-20 aient été satisfaites au moment de la signature ;"
					,singleOperation.isQscsc()
					,tableConditions
			);
			//cell 7
			Phrase concatenatedPhrase = new Phrase();
			concatenatedPhrase.add(new Chunk("L’intégrité des données signées n’ait pas été compromise\n", fontTenSimple));
			concatenatedPhrase.add(new Chunk("*** Des modifications non signées ont été apportées après l’apposition de la dernière signature", fontErr));
			String condition;
			
			if(!singleOperation.getIntegriteRes().equals("OUI")){
				condition = concatenatedPhrase.toString();
				//condition.substring(1, condition.length() - 1);
			}else{
				condition = new Phrase(new Chunk("L’intégrité des données signées n’ait pas été compromise",fontTenSimple)).toString();
			}
			checkControleReglementaire(
					 condition
					 ,singleOperation.getIntegriteRes().equals("OUI") && (singleOperation.getMail2()!=null || singleOperation.getMail1()!=null) && singleOperation.getResultlistpso().get("C")!=null && singleOperation.getResultList().get("UID")!=null
					 ,tableConditions
			);
			 //8
			checkControleReglementaire(
					 "Conditions prévues à l’article 5 de la loi 43-20 aient été satisfaites au moment de la signature"
					,singleOperation.getIntegriteRes().equals("OUI") && (singleOperation.getMail2()!=null || singleOperation.getMail1()!=null) && singleOperation.getResultlistpso().get("C")!=null && singleOperation.getResultList().get("UID")!=null
					,tableConditions
			 );
			 doc.add(tableConditions);

			 //remarque
			 Chunk NSt = new Chunk("Niveau de sécurité : ", fontTenSimpleBlue);
			 Chunk NStr = new Chunk("Signature "+niveau,niveau.equals("Qualifiée")? fontTenSimple :fontErr);

			 Phrase phrase = new Phrase();
			 phrase.add(new Chunk("\n"));
			 phrase.add(new Chunk("\n"));

			 doc.add(NSt);
			 doc.add(NStr);
			 doc.add(phrase);

			 /*end controle Detaille*/
			 /*start conclusion*/

			 Chunk chunkConcl = new Chunk("CONCLUSION", fontNineBoldBlue);
			 doc.add(chunkConcl);

			 // Add a line below the title
			 //title
			 Paragraph lineConcl = new Paragraph(new Chunk(new LineSeparator()));
			 lineConcl.setSpacingBefore(-12f);
			 doc.add(lineConcl);

			 //body
			 BaseColor bgColorConcl = new BaseColor(255, 242, 204);
			 PdfPTable tableConcl = new PdfPTable(1);
			 tableConcl.setWidthPercentage(100);
			 tableConcl.getDefaultCell().setBorder(0);
			 tableConcl.setSpacingAfter(10f);

			 Paragraph paragraphConcl = new Paragraph("Certificat de signature qualifiée mais la signature électronique n’est pas qualifiée car elle a subi des modifications post-apposition de la signature");
			 paragraphConcl.setFont(fontConclBody);
			 paragraphConcl.setSpacingAfter(5f);

			 PdfPCell cellConcl = new PdfPCell();
			 cellConcl.setBackgroundColor(bgColorConcl);
			 cellConcl.addElement(paragraphConcl);
			 cellConcl.setVerticalAlignment(Element.ALIGN_MIDDLE);
			 cellConcl.setBorder(0);

			 tableConcl.addCell(cellConcl);

			 doc.add(tableConcl);
			 
			 
		
		}

		doc.close();
		
		String statusRapport=null;
		if(usagePrevu.equals("PASSED")) {
			statusRapport="Signatures qualifiée conforme à la loi 43-20";
		}else {
			statusRapport="Signatures ne conforme pas à la loi 43-20";
		}
		
		String nomDocVer = nomDocVerifie;

		PdfGenerator pdf = new PdfGenerator();
		pdf.setNom_file(nomDocVer);
		pdf.setDate(currentDateGenerationReport);
		pdf.setHach_doc(Operations.getEmpreintDocument(file));
		pdf.setStatus_rapp(statusRapport);
		pdf.setUser(null);
		pdf.setRapport(generatedReportByte.toByteArray());

		insertIntoDB(pdf);

		return pdf;
		//return new ByteArrayInputStream(out.toByteArray());
	}

	public PdfGenerator insertIntoDB(PdfGenerator pdf) {
		return generatePdfRepository.save(pdf);
	}
}