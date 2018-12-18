IntelliSearch
-

[![Build Status](https://travis-ci.com/RogerJFX/MicroSearch.svg?branch=master)](https://travis-ci.com/RogerJFX/MicroSearch)
[![Coverage Status](https://codecov.io/gh/rogerjfx/microsearch/branch/master/graph/badge.svg)](https://codecov.io/gh/RogerJFX/MicroSearch)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Another attempt to make search results most reliable.**

History of naming of this project is:

- Phonetic Search
- Reliable Search
- Micro Search
- IntelliSearch

There is a particular hierarchy here. 

- Phonetic makes simple searches more reliable in most cases. 
- A reliable search only should be reliable after passing some search processes. Filtering, mapping, whatever.
- Making search processes micro services has its own particular charm. So we can divide Lucene indices up and 
combine or filter results in the end.
- We added some very simple machine learning processes. So we decided to call it IntelliSearch

Since there is a tremendous change of mind within this project, you should be patient. 