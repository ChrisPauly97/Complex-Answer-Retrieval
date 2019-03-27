from flask_wtf import FlaskForm
from wtforms import TextAreaField, SubmitField, IntegerField, RadioField
from wtforms.validators import DataRequired, NumberRange, Optional
from flask_wtf.file import FileField


class QueryForm(FlaskForm):
    queryText = TextAreaField('Query Text', validators=[DataRequired()])
    numOfParagraphs = IntegerField('Paragraphs to return',
                                   validators=[DataRequired(), NumberRange(max=1000)])
    numExpansionTerms = IntegerField('Num Expansion Terms')
    expansionWords = TextAreaField("Manual Terms")
    expansionModel = RadioField('Expansion model:', choices=[('Glove', 'Glove Independent '),
                                                             ('ELMoAvg', 'ELMo Average'),
                                                             ('ELMoIndy', 'ELMo Independent'),
                                                             ('noContext', 'ELMo Contextless'),
                                                             ('Global', 'ELMo Global'),
                                                             ('RM', 'Relevance Feedback'),
                                                             ('none', 'No Expansion Model')]
                                , validators=[Optional()])
    file = FileField('Expansion Term File Upload')
    rankOrEval = RadioField('Evaluate or Rank:', choices=[('eval', "Evaluate"),
                                                          ('rank', "Rank")])
    submit = SubmitField('Send Query')
