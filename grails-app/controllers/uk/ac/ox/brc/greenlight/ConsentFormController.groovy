package uk.ac.ox.brc.greenlight

class ConsentFormController {

    def consentFormService

    def find()
    {
        def result= consentFormService.search(params);
        render view:"search", model:[consentForms:result]
    }

}
