import unittest

from flaskCAR import init
from queryEngine_pb2 import QueryResponse

class TestApp(unittest.TestCase):

    def setUp(self):
        app = init()

        app.testing = True
        self.app = app.test_client()

    def test_query_request(self ):
        req = query_server('enwiki:Agriprocessors/Controversies/Animal%20abuse', 1000)
        assert req.queryFormat == "rih"
        assert req.numberOfDocs == 1000
        assert req.query == 'enwiki:Agriprocessors/Controversies/Animal%20abuse'
        assert req.batchType == "combine"

    def test_format_response(self):
        queryResponse = QueryResponse()
        p = "in january __NUMBER__ __NUMBER__ __NUMBER__ __NUMBER__ there was some controversy over donations made" \
            " to katherine harris by members of balkanys extended family including the ceo of agriprocessors harris" \
            " refused to return the donations organised by balkany after being made aware of the __NUMBER__ " \
            "__NUMBER__ __NUMBER__ __NUMBER__ prosecution against balkany and the peta investigation into allegations" \
            " of animal cruelty at agriprocessors"

        queryResponse.rprec = 0.3
        queryResponse.map = 0.02
        queryResponse.ndcg = 0.001
        queryResponse.paragraph.append(p)
        queryResponse.qrelParagraph.append(p)
        form = QueryForm()
        form.queryText.data = 'enwiki:Agriprocessors/Controversies/Animal%20abuse'
        form.numOfParagraphs.data = 5
        response = format_response(queryResponse, form)

        test = [form.queryText.data, p, ' '.join(["0.02", "0.00", "0.30"]), form.numOfParagraphs.data, p]
        assert response == test

    def format_response(self, form):
        formatted = []
        text = form.queryText.data
        paragraph_num = form.numOfParagraphs.data
        eval_results = " ".join(("{0:.2f}".format(self.map),
                                 "{0:.2f}".format(self.ndcg),
                                 "{0:.2f}".format(self.rprec)))
        paragraphs_string = ",".join(self.paragraph)
        qrel_paragraphs = ",".join(self.qrelParagraph)
        x = 5
        if paragraph_num > x:
            p_num = x
        else:
            p_num = paragraph_num
        formatted.extend((text, paragraphs_string, eval_results, p_num, qrel_paragraphs))
        return formatted
