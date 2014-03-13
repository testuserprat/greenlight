package uk.ac.ox.brc.greenlight

import grails.converters.JSON
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.tomcat.jni.Shm
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class AttachmentController {


    def attachmentService

    def list()
    {
        def result = attachmentService.getAllAttachments()
        respond result  , model:[attachments:result ]
    }



    def index() {
        redirect action:'list'
    }

    def show(Attachment attachment) {
       respond attachment,model: [attachment: attachment]
    }

    def delete() {
        def attachment = Attachment.get(params?.id);
        if(!attachment)
        {
            render "not found"
        }

        if(!attachment.consentForm)
        {
            attachment.delete flush:true
            def result=[id:attachment.id, status:'success'];
            render result as JSON
        }
        else
        {
            render 'Can not delete it as it\'s annotated'
        }
    }


    def viewContent = {
        byte[] content= attachmentService.getContent(params?.id);
        response.outputStream << content
    }

    def viewPDF ={
        byte[] content= attachmentService.getContent(params?.id);
        def data = "data:application/pdf;base64,${content.encodeBase64().toString()}"
        def result=[content:data]
        render result as JSON
    }

    def create() {
    }

    def showUploaded() {
    }

    def save()
    {
        def attachments = []
        if(request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest)request;
            List<MultipartFile> files = multiRequest.getFiles("scannedForms");

			files.each{ MultipartFile file ->
                def okContentTypes = ['image/png', 'image/jpeg', 'image/pjpeg', 'image/jpg', 'image/gif','application/pdf'];
                def confType=file.getContentType();
                if (okContentTypes.contains(confType) && file.size > 0){

					// If it's a PDF split the pages out and convert to an image first
					if(confType=='application/pdf'){
						PDDocument document = PDDocument.load(file.inputStream)
						document.getDocumentCatalog().getAllPages().eachWithIndex{ PDPage page, pageNumber ->
							BufferedImage bufferedImage = page.convertToImage()
							ByteArrayOutputStream baos = new ByteArrayOutputStream()
							ImageIO.write(bufferedImage, "jpg", baos)

							def attachment= new Attachment();
							attachment.fileName = file?.originalFilename + "_p" + pageNumber
							attachment.content = baos.toByteArray()
							attachment.attachmentType=Attachment.AttachmentType.IMAGE
							attachment.dateOfUpload=new Date()
							attachmentService.save(attachment)
							attachments.add(attachment)
						}

					}
					else{
						def attachment= new Attachment();
						attachment.fileName = file?.originalFilename
						attachment.content = file?.bytes
						attachment.attachmentType=Attachment.AttachmentType.IMAGE
						attachment.dateOfUpload=new Date()
						attachmentService.save(attachment)
						attachments.add(attachment)
					}
				}
            }
        }else{
			log.error("Attachment save request isn't multipart, ignoring")
		}
        render view: 'showUploaded',model:[attachments:attachments]
    }

}