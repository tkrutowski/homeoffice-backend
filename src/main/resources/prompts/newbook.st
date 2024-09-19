Wyszukaj  informacje o  książce z podanego linku :https://lubimyczytac.pl/ksiazka/4863130/ksiezyce-monarchy  i zwróć te informacje w postaci json.

Sprawdz czy wygenerowany link do okładki (cover) na pewno wskazuje na książkę z podanego linku.

Jeżeli książka należy do jakiejś serii to:
- gdy nie ma opisu tej serii (series/discripotion) na podanej stronie  to poszukaj gdzie indziej.
- upewnij się, że wygenerowany url (series/url) na pewno prowadzi do książek z tej serii


Nie odpowiadaj dopóki nie sprawdzisz dokładnie. Jeżeli nic nie znajdziesz zwróć pusty json.

Format json do usupełnienia:
[{ 
    "series": { 
        "title": <TYTUŁ SERII>, 
        "description": <OPIS SERII>,
        "url": <URL DO SERII>
    },
    "authors": [
        {
            "firstName": <FIRST NAME>, 
            "lastName": <LAST NAME> 
        }
    ],
    "categories": [
        { 
            "name": <CATEGORY np. SCIENCE FICTION> 
        },
    ],
    "title": <TYTUŁ>, 
    "description": <OPIS>, 
    "cover": <LINK DO OKŁADKI>, 
    "bookInSeriesNo": <NR KSIĄŻKI W SERII JEŻELI KSIĄŻKA JEST W SERII>
    "url":<URL DO KSIĄŻKI>
}]