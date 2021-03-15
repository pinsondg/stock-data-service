import csv


with open('../src/main/resources/data/supported_tickers.csv', 'r', newline='') as csv_file:
    valid_rows = []
    stock_reader = csv.reader(csv_file)
    for num, row in enumerate(stock_reader):
        if num == 0:
            valid_rows.append(row)
        elif (row[1] == 'NASDAQ' or "NYSE" in row[1]) and (row[4] and row[5]):
            valid_rows.append(row)
    print('Valid Tickers Found:' + str(len(valid_rows)))
with open('../src/main/resources/data/supported_tickers.csv', 'w', newline='') as write_file:
    stock_writer = csv.writer(write_file)
    print("Writing to file " + write_file.name)
    stock_writer.writerows(valid_rows)
