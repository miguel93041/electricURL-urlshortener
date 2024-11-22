$(document).ready(function () {
    $("#uploadButton").click(showFileDialog);
    $("#fileInput").change(handleFileChange);
    $("#shortener").submit(handleFormSubmit);
});

function showFileDialog() {
    $("#fileInput").click();
}

function handleFileChange() {
    if (this.files.length > 0) {
        $("#urlInput").prop("disabled", true);
        $("#shortener").submit();
    } else {
        $("#urlInput").prop("disabled", false);
    }
}

function handleFormSubmit(event) {
    event.preventDefault();

    if ($("#fileInput").prop("files").length > 0) {
        processFileUpload();
    } else {
        processUrlShortening();
    }
}

function processFileUpload() {
    const file = $('#fileInput')[0].files[0];

    if (file.name.endsWith('.csv')) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('qrRequested', $('#qrCheckbox').is(':checked'));

        $.ajax({
            type: "POST",
            url: "/api/upload-csv",
            data: formData,
            processData: false,
            contentType: false,
            success: handleFileUploadSuccess,
            error: handleError
        });
    } else {
        showErrorMessage("Por favor, sube un archivo CSV.");
    }
}

function processUrlShortening() {
    $.ajax({
        type: "POST",
        url: "/api/link",
        data: $("#shortener").serialize(),
        success: handleUrlShorteningSuccess,
        error: handleError
    });
}

function handleFileUploadSuccess(data) {
    const url = window.URL.createObjectURL(new Blob([data]));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'shortened_urls.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    resetForm();
}

function handleUrlShorteningSuccess(msg, status, request) {
    const shortenedUrl = request.getResponseHeader('Location');

    let resultHtml = `
        <div class='alert alert-success lead'>
            <a target='_blank' href='${shortenedUrl}'>${shortenedUrl}</a>
    `;

    if (msg.qrCodeUrl) {
        resultHtml += `
            <br><br>
            View QR Code at:
            <a href="${msg.qrCodeUrl}" target="_blank">${msg.qrCodeUrl}</a>
        `;
    }

    resultHtml += `</div>`;

    $("#result").html(resultHtml);

    resetForm();
}

function handleError() {
    showErrorMessage("ERROR");
}

function showErrorMessage(message) {
    $("#result").html(`<div class='alert alert-danger lead'>${message}</div>`);
}

function resetForm() {
    $("#urlInput").val('').prop("disabled", false);
    $("#fileInput").val('');
    $("#qrCheckbox").prop("checked", false);
}
