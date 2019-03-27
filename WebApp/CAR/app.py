from collections import defaultdict

from flask import Flask, render_template, flash
import re
import grpc
from werkzeug.datastructures import FileStorage
import queryEngine_pb2_grpc, settings
from forms import QueryForm
from queryEngine_pb2 import QueryRequest


def init():
    server = Flask(__name__)
    return server


app = init()
app.config['SECRET_KEY'] = '156aab61c96e25678e39d6a011ead45f'


# @app.route("/")
# @app.route("/home")
# @app.route("/about")
# def home():
#     return render_template("home.html", title="Home")


# Sending a query via the form is done through this route.
# The form has a default query of 'enwiki:Wildlife/Destruction/Chains%20of%20extinction'
# The redirect is based on evaluation or ranking terms.
# It is then considered whether an expansion model is used.
# Either rank function or send_request functions are called.
@app.route("/", methods=['GET', 'POST'])
@app.route("/query", methods=['GET', 'POST'])
def query_from_form():
    form = QueryForm(queryText='enwiki:Antibiotics/Medical%20uses/Administration')
    if form.is_submitted() and form.validate():

        #       ----------------- EVAL -----------------

        if form.rankOrEval.data == "eval":
            formatted_response = send_request(form)
            if formatted_response == 0:
                flash(f'Query {form.queryText.data} failed!', 'danger')
                return render_template("queries.html", title="Query", form=form)
            if form.expansionModel.data == "none" and form.expansionWords.data == "" and form.file.data is None:
                flash(f'Query {form.queryText.data} submitted!', 'success')
                return render_template("singleQuery.html", query=formatted_response)
            else:
                flash(f'Query {form.queryText.data} submitted!', 'success')
                return render_template("expandedQuery.html", query=formatted_response)

        #       ----------------- RANK -----------------

        elif form.rankOrEval.data == "rank":
            ranked_results = ranking_query_form(form)

            if ranked_results == 0:
                flash(f'Query {form.queryText.data} failed!', 'danger')
                return render_template("queries.html", title="Query", form=form)

            flash(f'Query {form.queryText.data} submitted!', 'success')
            return render_template("rankView.html", title="Query", response=ranked_results[0],
                                   good_word_map=ranked_results[1], count=ranked_results[2], original=ranked_results[3])


    #   ----------------- Failure -----------------

        flash(f'Query {form.queryText.data} failed!', 'danger')
    return render_template("queries.html", title="Query", form=form)


# Function to generate a query using the given expansion model.
# The first conditional block returns a list of expansion terms from the chosen model
# If no model is chosen, or the model finds no expansion terms, an error value is returned.
# A map of expansionTerm to 0,1 based on relevancy is created, this can be used for offline analysis of expansion terms.

def ranking_query_form(form):
    cleaned_query_text = form.queryText.data \
        .replace("enwiki:", "") \
        .replace("/", " ") \
        .replace("%20", " ") \
        .replace("-", " ") \
        .lower() \
        .strip()

    expansion_words = []
    word_map = create_word_map()
    unedited_query = "#combine(" + cleaned_query_text + ")"

    #   ----------------- Glove -----------------

    if form.expansionModel.data == "Glove":
        for word in set(cleaned_query_text.split(" ")):
            if word in word_map and word not in load_stop_words():
                for x in word_map[word]:
                    expansion_words.append(x)

        expansion_words = sorted(expansion_words, key=take_sim)
        expansion_words = [x.split("+")[0] for x in expansion_words]

        if not expansion_words:
            flash('Form Submission Failure. No Useful Glove expansion words found for this query', 'danger')
            return render_template("queries.html", title="Query", form=form)

    elif form.expansionModel.data == "RM":
        rm_term_map = create_rmterm_map()

        if unedited_query in rm_term_map.keys():
            expanded_words = rm_term_map[unedited_query]
        else:
            return 0

        expansion_words.append(expanded_words[:form.numExpansionTerms.data])


    #   ----------------- ELMo -----------------
    elif form.expansionModel.data == "ELMoAvg" or form.expansionModel.data == "ELMoIndy":

        indy_and_avg = get_elmo_queries(True)

        if form.expansionModel.data == "ELMoAvg":

            if not indy_and_avg[1].__contains__(form.queryText.data):
                return 0
            else:
                similar_words = indy_and_avg[1][form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_words.append(word[0])

        elif form.expansionModel.data == "ELMoIndy":

            if not indy_and_avg[0].__contains__(form.queryText.data):
                return 0
            else:
                similar_words = indy_and_avg[0][form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_words.append(word[0])
        print(expansion_words)

    #   ----------------- noContext or Global -----------------

    elif form.expansionModel.data == "noContext" or form.expansionModel.data == "Global":

        noContextPath = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Local_ELMo/noContextWeightsTrain.txt"
        globalPath = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Global_ELMo/global_avg_similarity_test.txt"

        if form.expansionModel.data == "noContext":

            no_context_and_global = get_more_elmo(noContextPath)

            if not no_context_and_global.__contains__(form.queryText.data):
                return 0
            else:
                similar_words = no_context_and_global[form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_words.append(word[0])

        elif form.expansionModel.data == "Global":

            no_context_and_global = get_more_elmo(globalPath)

            if not no_context_and_global.__contains__(form.queryText.data):
                return 0
            else:
                similar_words = no_context_and_global[form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_words.append(word[0])

    #   ----------------- File -----------------

    elif form.file.data is not None:
        file_handle = FileStorage(stream=form.file.data)
        fileContents = file_handle.stream.read()
        expansion_words = fileContents.decode('UTF-8').rstrip("\n").split("\n")


    #   ----------------- None -----------------

    elif form.expansionModel.data == "none" and form.expansionWords.data == "":
        expansion_words = []


    #   ----------------- Manual -----------------

    else:
        expansion_words.append(form.expansionWords.data.strip())


#   ----------------- None -----------------

    if expansion_words:

        # Function to build the queries, get the results, and rankings of terms
        def run_ranking_queries(e_words, s_form, u_query):

            r_list = []
            e_word_map = dict()

            qr = QueryRequest()
            qr.rawQueryText = s_form.queryText.data
            qr.originalQuery = u_query
            qr.numberOfDocs = s_form.numOfParagraphs.data
            res = send_req(qr)
            o_query_formatted = format_query_response(res.queryEval, res.Results, "", s_form, False, qr.originalQuery)
            r_list.append(o_query_formatted)


            for w in ' '.join(e_words).split(" "):

#               ----------------- Build Request -----------------

                qr = QueryRequest()

                qr.rawQueryText = s_form.queryText.data

                if w != "":
                    qr.originalQuery = "#combine:0=0.8:1=0.2(" + u_query + " #combine( " + w + " ))"
                else:
                    qr.originalQuery = u_query

                qr.numberOfDocs = s_form.numOfParagraphs.data

                res = send_req(qr)

#               ----------------- Format Response -----------------


                o_query_formatted = format_query_response(res.queryEval, res.Results, w, s_form, True, qr.originalQuery)

                r_list.append(o_query_formatted)

                original_eval_list = o_query_formatted[1].split(" ")
                print(res.queryEval)

                good_expansion_word = False

                for i, j in zip(original_eval_list, original_eval_list):
                    if j > i:
                        print(w + " is a good expansion word")
                        good_expansion_word = True

                e_word_map[w] = good_expansion_word

            return e_word_map, r_list, o_query_formatted

        rankedQueryTerms = run_ranking_queries(expansion_words, form, unedited_query)
        expansion_word_map = rankedQueryTerms[0]
        response_list = rankedQueryTerms[1]
        original_query_formatted = rankedQueryTerms[2]

#       ----------------- Logging Interesting Values -----------------

        count_good_expansion_words = sum(value == True for value in expansion_word_map.values())
        count_bad_expansion_words = len(expansion_word_map.keys()) - count_good_expansion_words

        print("No. of good expansion words: " + str(count_good_expansion_words))
        print("No. of bad expansion words: " + str(count_bad_expansion_words))
        print("Percentage of Good Words: " + "%.2f" % (
                count_good_expansion_words / (count_bad_expansion_words + count_good_expansion_words)))

    else:
        return 0

    flash(f'Query {form.queryText.data} submitted!', 'success')
    return [response_list, expansion_word_map, count_good_expansion_words, original_query_formatted]


# Function to create stub, channel, send request, and receive response
def send_req(request):
    channel = grpc.insecure_channel('localhost:50051')
    stub = queryEngine_pb2_grpc.QueryEngineStub(channel)
    query_response = stub.sendQuery(request)
    return query_response


# Turn the response into the object given to the html
def format_query_response(ev, query_res, expanded_without_query, form, expanded_bool, expanded_query_string):
    query_paragraphs_list = []
    query_similarity_list = []
    stop_words = load_stop_words()
    expanded_match_count = 0


#   ----------------- Build Query text -----------------

    query_title = form.queryText.data.replace("enwiki:", "")

    leaf_heading = '_'.join(query_title.split("/")[-1].split("%20"))
    url_text = query_title.split("/")[0] + "#" + leaf_heading
    url_text = "<a class=queryLink btn btn-info href=http://wikipedia.com/wiki/" + url_text + ">" + query_title.replace(
        "%20", " ") + "</a>"


#   ----------------- Handle Expansion Terms -----------------

    word_list = expanded_without_query

    all_paragraphs = ""

    for item in query_res:
        all_paragraphs += item.paragraph

    newWordList = ""

    eval_results = " ".join(("{0:.2f}".format(ev.map),
                             "{0:.2f}".format(ev.ndcg),
                             "{0:.2f}".format(ev.rprec)))

    if expanded_bool:
        for x in range(len(' '.join(word_list).split(" "))):

            currentWord = word_list[x]
            # Mark stop words
            if currentWord in stop_words:
                currentWord = "<span class=stopWord>" + currentWord + "</span>"
            # If word exists in any word in all of the paragraphs mark it as a matched word
            elif currentWord in all_paragraphs.split(" "):
                currentWord = "<span class=matchedWord>" + currentWord + "</span>"
            # Otherwise it's unmatched
            else:
                currentWord = "<span class=unmatchedWord>" + currentWord + "</span>"

            newWordList += " " + currentWord

            if form.expansionModel.data == "Glove":
                expanded_words = url_text + " " + newWordList
                expanded_words.strip()
            else:
                expanded_words = url_text + " " + form.expansionWords.data.strip()
        url_text = expanded_words

    original_query = query_title.replace("%20", " ").lower().replace("/", " ")

    for item in query_res:

        item.paragraph = item.paragraph.replace(".", " ")
        item.paragraph = re.sub(r"<link.*>(.*)</link>[\s\S]+?<page.*>(.*)</page>",
                                r"( <a class=\"btn\" href=http://wikipedia.com/wiki/\1> \1 </a>)", item.paragraph)

        # Word list is a list of all words in the paragraph.
        word_list = item.paragraph.split(" ")

        # For each word in the paragraph
        for x in range(len(word_list)):

            if re.sub("[,.\'\"!?/;:\n]", "", word_list[x].lower()) in stop_words:
                word_list[x] = "<span class=stopWord> " + word_list[x] + " </span>"

            elif re.sub("[,.\'\"!?/;:\n]", "", word_list[x].lower()) in original_query:
                word_list[x] = "<span class=queryWord> " + word_list[x] + " </span>"

            elif expanded_bool:

                for y in range(len(expanded_without_query)):

                    # If the word is in only the expansion words
                    if re.sub("[,.\'\"!?/;:\n]", "", word_list[x].lower()) in expanded_without_query:
                        word_list[x] = "<span class=matchedWord>" + word_list[x] + "</span>"
                        expanded_match_count += 1

        query_paragraphs_list.append(' '.join(word_list))
        query_similarity_list.append(item.similarity)

    paragraph_num = form.numOfParagraphs.data

    x = 5
    if paragraph_num > x:
        p_num = x
    else:
        p_num = paragraph_num

    return [zip(query_paragraphs_list, query_similarity_list), eval_results, url_text, p_num, expanded_match_count,
            expanded_query_string]


# def get_expansion_terms(query,num_terms):


def send_request(form):
    word_map = create_word_map()
    expansion_list = []

    cleaned_query_text = form.queryText.data.replace("enwiki:", "").replace("/", " ").replace("%20", " ").lower()
    unedited_query = "#combine(" + cleaned_query_text + ")"

    if form.expansionModel.data == "Glove":

        for word in set(cleaned_query_text.replace("-", " ").split(" ")):
            if word in word_map:
                split_words = sorted(word_map[word], key=take_sim)
                print(split_words)
                for x in split_words:

                    expansion_list.append(x)

        expansion_list = [x.split("+")[0] for x in sorted(expansion_list, key=take_sim)][:10]
        expanded_without_query = expansion_list

    #   ----------------- ELMo -----------------
    elif form.expansionModel.data == "ELMoAvg" or form.expansionModel.data == "ELMoIndy":

        indy_and_avg = get_elmo_queries(True)

        if form.expansionModel.data == "ELMoAvg":

            if not indy_and_avg[1].__contains__(form.queryText.data):
                return 0
            else:
                similar_words = indy_and_avg[1][form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_list.append(word[0])
                    expanded_without_query = expansion_list
        elif form.expansionModel.data == "ELMoIndy":

            if not indy_and_avg[0].__contains__(form.queryText.data):
                return 0
            else:
                similar_words = indy_and_avg[0][form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_list.append(word[0])
                    expanded_without_query = expansion_list

    #   ----------------- noContext or Global -----------------

    elif form.expansionModel.data == "noContext" or form.expansionModel.data == "Global":

        noContextPath = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Local_ELMo/noContextWeightsTrain.txt"
        globalPath = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Global_ELMo/global_avg_similarity_test.txt"

        if form.expansionModel.data == "noContext":

            no_context_and_global = get_more_elmo(noContextPath)

            if not no_context_and_global.__contains__(form.queryText.data):
                return 0
            else:
                similar_words = no_context_and_global[form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_list.append(word[0])
                    expanded_without_query = expansion_list

        elif form.expansionModel.data == "Global":

            no_context_and_global = get_more_elmo(globalPath)

            if not no_context_and_global.__contains__(form.queryText.data):
                return 0
            else:
                similar_words = no_context_and_global[form.queryText.data]
                for word in similar_words[:form.numExpansionTerms.data]:
                    expansion_list.append(word[0])
                    expanded_without_query = expansion_list

    elif form.file.data is not None:

        filehandle = FileStorage(stream=form.file.data)
        fileContents = filehandle.stream.read()
        fileContents = fileContents.decode('UTF-8')
        expansion_list = fileContents.split("\n")
        expanded_without_query = expansion_list
    else:
        expanded_without_query = form.expansionWords.data.strip().split(" ")

        for q in form.expansionWords.data.strip().split(" "):
            expansion_list.append(q)

    oQ = 0.8
    eQ = 0.2

    print("Manually Expanded Query: " + "#combine:0=" + str(oQ) + ":1=" + str(
        eQ) + "(" + unedited_query + " #combine(" + ' '.join(expansion_list) + "))")


    # -------------------- Query Request ------------------------

    query_request = QueryRequest()
    query_request.rawQueryText = form.queryText.data
    query_request.originalQuery = unedited_query
    query_request.numberOfDocs = form.numOfParagraphs.data

    query_response = send_req(query_request)
    original_query_formatted = format_query_response(query_response.queryEval,
                                                     query_response.Results,
                                                     "",
                                                     form, False, query_request.originalQuery)
    query_request = QueryRequest()
    query_request.rawQueryText = form.queryText.data
    query_request.numberOfDocs = form.numOfParagraphs.data

# format_query_response(res.queryEval, res.Results, w, s_form, True, qr.originalQuery)
    query_request.originalQuery = "#combine:0=" + str(oQ) + ":1=" + str(eQ) + \
                                  "(" + unedited_query + "#combine(" + ' '.join(expansion_list) + "))"

    query_response = send_req(query_request)

    expanded_query_formatted = format_query_response(query_response.queryEval,
                                                     query_response.Results,
                                                     expanded_without_query,
                                                     form, True, query_request.originalQuery)



    # ---------------------- Manage Qrels -----------------------

    qrel_paras = query_response.qrelParagraph
    paragraph_string = ""
    expanded_qrel_match = 0
    qrel_para_lengths = []

    # Only process the qrels if they exist
    if qrel_paras:

        for x in range(len(qrel_paras)):
            para = re.sub("<page(.*?)</page>", "", qrel_paras[x])
            para = re.sub("<link(.*?)</link>", "", para)
            para = re.sub("<a(.*?)</a>", "", para)
            para = re.sub("[\n \t]+", " ", para)
            para = para.replace(",", "")

            qrel_para_lengths.append(len(para.split(" ")))

            for y in range(qrel_para_lengths[x]):

                if re.sub("[,.\'\"!?/;: ]", "", para.split(" ")[y]) \
                        in expanded_without_query \
                        and re.sub("[,.\'\"!?/;:]", "", para.split(" ")[y]) not in load_stop_words():

                    paragraph_string += "<span class=qrelMatch>" + para.split(" ")[y] + "</span> "
                    expanded_qrel_match += 1
                    continue

                elif re.sub("[,.\'\"!?/;:]", "", para.split(" ")[y]) in cleaned_query_text.split(" ") \
                        and re.sub("[,.\'\"!?/;: ]", "", para.split(" ")[y]) not in load_stop_words():
                    paragraph_string += "<span class=queryWord>" + para.split(" ")[y] + "</span> "
                    continue

                else:
                    paragraph_string += para.split(" ")[y] + " "

            paragraph_string.strip()
            paragraph_string += ","
            paragraph_string = paragraph_string.strip()

    return [original_query_formatted, expanded_query_formatted, paragraph_string[:-1], expanded_qrel_match]


# Generates a map of each word to the top n most similar words using a file generated by the jupyter notebook.
def create_word_map():
    with open("data/topIN50.txt", "r") as top_words:
        words = top_words.readlines()[0]
        top_words = re.sub("[][,']", "", words).strip()

    word_map = defaultdict(list)

    for mapPair in top_words.split(";"):
        map_pair_split = mapPair.split(":")

        if len(map_pair_split) > 1:

            split_words = map_pair_split[1].split(" ")

            split_words = sorted(split_words, key=take_sim)

            for word in split_words:
                word_map[map_pair_split[0]].append(word)

    return word_map


def get_elmo_queries(test):
    if(test):
        with open("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Local_ELMo/embeddingsAverageWeightsUnlimitedTest.txt") as embeddings:
            data = embeddings.readlines()
    else:
        with open("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Local_ELMo/embeddingsAverageWeightsUnlimited.txt") as embeddings:
            data = embeddings.readlines()

    indyWordsToQuery = dict()
    avgWordsToQuery = dict()

    for line in data:
        entireSet = []
        indySet = []

        queryId = line.split("@")[0]
        expansionWords = line.split("@")[1]

        splitLine = re.sub("[\[\]']", "", expansionWords).split(",")

        for s in splitLine:

            outSet = entireSet if (s.split(":")[0].strip() == "entireQuery") else indySet

            for w in s.split(":")[1].split(" "):

                weightList = w.split("+")

                if weightList[0].lower() not in queryId.lower():
                    entireWeight = (weightList[0],weightList[1])
                    outSet.append(entireWeight)

        def srt(ls): return sorted(ls, key=lambda k:k[1])

        indyWordsToQuery[queryId] = srt(indySet)
        avgWordsToQuery[queryId] = srt(entireSet)

    return indyWordsToQuery, avgWordsToQuery


def get_more_elmo(path):
    with open(path) as f:
        data = f.readlines()
    noContextWordsToQuery = dict()

    for l in data:

        queryId = l.split("@")[0]
        expansionWords = l.split("@")[1]
        splitLine = re.sub("[\[\]']", "", expansionWords).split(",")
        expansion_word_set = []

        for s in splitLine:

            entireWordWeightList = s.split("+")

            if not queryId.lower().__contains__(entireWordWeightList[0].strip().lower()):
                wordWeight = (entireWordWeightList[0].strip(), entireWordWeightList[1])
                expansion_word_set.append(wordWeight)

        noContextWordsToQuery[queryId] = expansion_word_set
    return noContextWordsToQuery


def take_sim(elem):
    return elem.split("+")[1]


def create_rmterm_map():
    with open("CAR/data/rm-terms.txt", "r") as rm_terms:
        lines = rm_terms.read()

    rm_terms_map = dict()
    query_id = ""
    query_terms = ""

    for line in lines.split("\n"):
        if len(line.split("@")) == 2:
            query_id = line.split("@")[0]
            query_terms = line.split("@")[1]

        rm_terms_map[query_id] = query_terms
    return rm_terms_map


def load_stop_words():
    with open("data/StopWords.txt", "r") as stop_words:
        words = stop_words.readlines()
        for x in range(len(words)):
            words[x] = words[x].replace("\n", "")
    return words


if __name__ == "__main__":
    app.run(
        host=settings.API_BIND_HOST,
        port=settings.API_BIND_PORT,
        debug=settings.DEBUG
    )
