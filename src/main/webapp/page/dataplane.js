const host = window.location.hostname;
const port = window.location.port;

function sendTask() {
    var payload = JSON.stringify({
        'taskType': $('#taskType').val(),
        'param': JSON.parse($("#param").val()),
        'ext': JSON.parse($("#ext").val()),
        'taskId': generateUUID()
    });
    console.info(payload);
    $.ajax({
        url: '/apis/create-controller/task/v1/add',
        type: 'POST',
        headers: {
            'accept': '*/*',
            'Content-Type': 'application/json'
        },
        data: payload,
        success: function(response) {
            showInfo('Success:' + JSON.stringify(response));
            $("#greetings").append("<tr><td>AddTask</td><td>" + response.data.taskId + "</td></tr>");
        },
        error: function(xhr, status, error) {
            showError('Error:' + status + error);
        }
    });
}

function showError(txt) {
    var alertBox = $('<div class="alert alert-danger alert-dismissible fade in" role="alert">' +
                           '<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
                           txt +
                         '</div>');

    $('#alertContainer').empty().append(alertBox);

    setTimeout(function() {
        alertBox.alert('close');
    }, 3000);
}

function showInfo(txt) {
    var alertBox = $('<div class="alert alert-info alert-dismissible fade in" role="alert">' +
                           '<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
                           txt +
                         '</div>');

    $('#alertContainer').empty().append(alertBox);

    setTimeout(function() {
        alertBox.alert('close');
    }, 3000);
}

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0,
            v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function sampleCozeWorkflow() {
    var sample =
`{
    "input": "Come Back to You"
}`;
    $("#param").val(sample);
    $("#ext").val(`{"workflowId": "7463480937172418614"}`);
    $("#taskType").val("CozeWorkflow");
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#send" ).click(() => sendTask());
    $( "#getTask" ).click(() => getTask());
    $( "#cw" ).click(() => sampleCozeWorkflow());
});