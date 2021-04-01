import lxml
from lxml.html.clean import Cleaner

cleaner = Cleaner()
cleaner.remove_unknown_tags = False
cleaner.javascript = True
cleaner.meta = True
cleaner.style = True

with open('/Users/dpinson/IdeaProjects/StockDataService/src/test/resources/mocks/yahoofinance/yahoo-finance-spy_clean.html', 'wb') as file:
    txt = lxml.html.tostring(cleaner.clean_html(lxml.html.parse('/Users/dpinson/IdeaProjects/StockDataService/src/test/resources/mocks/yahoofinance/yahoo-finance-spy.html')))
    file.write(txt)
