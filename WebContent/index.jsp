<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Enrichment</title>
</head>
<body>

<link rel="stylesheet" href="css/css.css">
<script src="scripts/jquery-3.1.1.min.js"></script>
<script src="scripts/control.js"></script>


<link rel="stylesheet" href="//code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css">

<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<link rel="stylesheet" href="https://cdn.datatables.net/1.10.16/css/dataTables.bootstrap.min.css">


<script src="https://code.jquery.com/jquery-1.12.4.js"></script>
<script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>

<script src="https://cdn.datatables.net/1.10.16/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.10.16/js/dataTables.bootstrap.min.js"></script>

<script src="scripts/enrichment.js"></script>

<link rel="stylesheet"  href="css/css.css">



<div style="padding: 30px;">

<%@include file="login.jsp" %>

<hr>

<h2>Gene Set Enrichment</h2>


<hr>

<div>

<div id="inputform">
<form action="enrichment" method="post" id="genelistform" onsubmit="return showBulb();">
<input type="hidden" name="mock" value="nothing"/>
<textarea rows="10" cols="30" name="text" default="Enter gene symbols" class="form-control input-lg" autocorrect="off">
TP53
TNF
EGFR
ESR1
HIF1A
BRCA1
AKT1
NFKB1
ERBB2
APP
STAT3
CDKN2A
PPARG
VDR
AR
PTEN
CTNNB1
BRCA2
CDH1
BCL2
CFTR
MYC
MTOR
MAPK1
SNCA
CDKN1A
MDM2
MAPT
CCND1
JAK2
BIRC5
FAS
NOTCH1
MAPK14
MAPK3
ATM
NFE2L2
ITGB1
SIRT1
LRRK2
IGF1R
GSK3B
RELA
CDKN1B
NR3C1
BAX
CASP3
JUN
SP1
RAC1
CAV1
RB1
PARP1
EZH2
RHOA
PGR
SRC
MAPK8
PTK2
STAT1
CASP8
PPARGC1A
AURKA
PSEN1
RUNX1
NPM1
TNFRSF1A
E2F1
TP73
MCL1
VHL
CHEK2
HRAS
PLK1
ABL1
BCL2L1
HTT
CD40
RAD51
YAP1
SMAD3
PRKCA
AHR
SMAD4
IRS1
DNMT1
PRKCD
HNF4A
PTPN11
CLU
HSP90AA1
RUNX2
BMI1
XIAP
SOCS3
CHEK1
KMT2A
PCNA
CDK1
TGFBR1
</textarea><br>
<input type="text" name="description" class="form-control input-lg" placeholder="Description"/><br>

<input type="submit" value="Submit list" class="btn btn-primary" tabindex="7" style="float: right;">
</form>
</div>


<div id="instructions">
	<p>Choose an input file to upload. Either in BED format or a
		list of genes. For a quantitative set, add a comma and the
		level of membership of that gene. The membership level is a
		number between 0.0 and 1.0 to represent a weight for each gene,
		where the weight of 0.0 will completely discard the gene from
		the enrichment analysis and the weight of 1.0 is the maximum.</p>
	<p>
		Try an example <a href="#" id="insertBedExample-link"
			onclick="return insertBedExample();">BED file</a>.
	</p>
</div>

</div>

<hr>

<div id="citation">
	Please acknowledge Enrichr in your publications by citing the
	following references: <br /> <a
		href="http://www.ncbi.nlm.nih.gov/pubmed/23586463"
		id='pubmedCitation-link' target="_blank">Chen EY, Tan CM, Kou
		Y, Duan Q, Wang Z, Meirelles GV, Clark NR, Ma&#39;ayan A. Enrichr:
		interactive and collaborative HTML5 gene list enrichment analysis
		tool. <i>BMC Bioinformatics. 2013;128(14)</i>.
	</a> <br />
	<br /> <a href="https://www.ncbi.nlm.nih.gov/pubmed/27141961"
		id='pubmedCitation-link' target="_blank"> Kuleshov MV, Jones
		MR, Rouillard AD, Fernandez NF, Duan Q, Wang Z, Koplev S, Jenkins
		SL, Jagodnik KM, Lachmann A, McDermott MG, Monteiro CD, Gundersen
		GW, Ma&#39;ayan A. Enrichr: a comprehensive gene set enrichment
		analysis web server 2016 update. <i>Nucleic Acids Research.
			2016; <a href=http://doi.org/10.1093/nar/gkw377>gkw377</a>
	</i>.
	</a>
</div>


<div id="bulb">
	<img src="images/bulbloading2.gif"><br><br>
	Illuminating...
</div>

<%@include file="footer.jsp" %>

</div>

</body>
</html>