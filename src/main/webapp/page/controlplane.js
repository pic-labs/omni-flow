function searchKind(kindId) {
    if(!kindId) {
        showError('Please enter a kindId.')
        return
    }
    $('#kindId').val(kindId);
    $.ajax({
        url: '/apis/create-controller/creator/v1/detail?kindId=' + kindId,
        method: 'GET',
        success: function(response) {
            if (response.code !== '00000') {
                showError('Failed to fetch status');
            }

            var status = response.data.status;
            let color = status.phase === 'FAILED' ? 'red' : 'green';
            $('#phaseOutput').empty().append(`Phase: `)
                .append(`<ul class="list-group"><li class="list-group-item ${getLiStyle(status.phase)}">${status.phase}</li></ul>`);
            $('#phaseOutput').append(`<hr>Conditions: </hr><ul class="list-group">`);
            $.each(status.condition, function(key, value) {
                $('#phaseOutput').append(`<li class="list-group-item ${getLiStyle(value)}">${key}: ${value}</li>`);
            });
            $('#phaseOutput').append(`</ul>`);
            if (status.phase === 'SUCCEED') {
                $('#phaseOutput').append(`<hr>EndStep: </hr><ul class="list-group">`);
                $('#phaseOutput').append(`<li style="font-weight: bold" class="list-group-item"><span>${convertLinks(JSON.stringify(status.endStep, null, 2))}</span></li></ul>`);
            }

            $('#regenerateSelect').empty();
            $.each(status.condition, function(key, value) {
                if (key !== 'start' && key !== 'end') {
                    $('#regenerateSelect').append(`<option value="${key}">${key}</option>`);
                }
            });
            $('#popup').show();
        },
        error: function() {
            showError('Failed to fetch status');
        }
    });
}
function convertLinks(text) {
    var urlPattern = /(https?:\/\/[^\s]+)/g;
    return text.replace(urlPattern, function(url) {
        return `<a href="${url}" target="_blank">${url}</a>`;
    });
}
function getLiStyle(status) {
    let st = '';
    switch(status) {
        case 'FAILED':
            st = 'list-group-item-danger';
            break;
        case 'RUNNING':
            st = 'list-group-item-info';
            break;
        case 'PENDING':
            st = '';
            break;
        case 'SUCCEED':
            st = 'list-group-item-success';
            break;
    }
    return st;

}

function kindDetail() {
    var kindId = $('#kindId').val().trim();
    if(!kindId) {
        showError('Please enter a kindId.')
        return
    }
    var url = '/apis/create-controller/operation/v1/detail?kindId=' + kindId;
    window.open(url, '_blank');
}

function generate() {
    var story = $('#story').val().trim();
     if(!story) {
        showError('Please enter a Kind definition.')
        return
    }

    $.ajax({
        url: '/apis/create-controller/creator/v1/create-flow',
        type: 'POST',
        headers: {
            'accept': '*/*',
            'Content-Type': 'application/json'
        },
        data: story,
        success: function(response) {
            showInfo('Success:' + response);
            var kId = response.data.id;
            $('#createOutput').empty().append(`<li style="font-weight: bold">kindId: ${kId}</li>`);
            $('#kindId').val(kId);
        },
        error: function(xhr, status, error) {
            showError('Error:' + status + error);
        }
    });
}

function regenerate() {
    var kindId = $('#kindId').val().trim();
     if(!kindId) {
        showError('Please enter a kindId.')
        return
    }

    var selectedValues = [$('#regenerateSelect').val()];

    if(selectedValues.length === 0) {
        showError('Please select regenerate Conditions.')
        return;
    }

    const data = {
        "kindId": kindId,
        "conditions": selectedValues
    };
    $.ajax({
        url: '/apis/create-controller/operation/v1/regenerate',
        type: 'POST',
        headers: {
            'accept': '*/*',
            'Content-Type': 'application/json'
        },
        data: JSON.stringify(data),
        success: function(response) {
            showInfo('Success:' + response);
        },
        error: function(xhr, status, error) {
            showError('Error:' + status + error);
        }
    });
}

let currentPage = 1;
const pageSize = 10;
function listKinds(page = currentPage, size = pageSize) {
    var uid = $('#uid').val().trim();
    if (!uid) {
        showError('Please enter a UserId.');
        return;
    }

    $.ajax({
        url: `/apis/create-controller/creator/v1/list?uid=${uid}&page=${page}&size=${size}`,
        method: 'GET',
        success: function(response) {
            if (response.code !== '00000') {
                showError('Failed to fetch kind list.');
                return;
            }

            var kinds = response.data.content;
            var totalItems = response.data.total;
            var totalPages = response.data.totalPages;
            var tbody = $('#kindListTable tbody');
            tbody.empty();

            kinds.forEach(function(kind) {
                var row = `<tr data-kind-id="${kind.id}" style="cursor: pointer">
                    <td>${kind.id}</td>
                    <td>${kind.phase}</td>
                    <td>${kind.kind}</td>
                    <td>${kind.createTime}</td>
                </tr>`;
                tbody.append(row);
            });

            $('#kindListTable tbody tr').click(function() {
                var kindId = $(this).data('kind-id');
                searchKind(kindId);
            });

            updatePagination(totalPages, totalItems);
        },
        error: function() {
            showError('Failed to fetch kind list.');
        }
    });
}

function updatePagination(totalPages, totalItems) {
    $('#totalItems').text(`Total Items: ${totalItems}`);

    const pagination = $('#pagination');
    pagination.empty();

    if (currentPage > 1) {
        pagination.append(`<li class="page-item"><a class="page-link" href="#" id="prevPage">Previous</a></li>`);
    } else {
        pagination.append(`<li class="page-item disabled"><a class="page-link" href="#" tabindex="-1" aria-disabled="true">Previous</a></li>`);
    }

    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            pagination.append(`<li class="page-item active"><a class="page-link" href="#">${i}</a></li>`);
        } else {
            pagination.append(`<li class="page-item"><a class="page-link" href="#" onclick="goToPage(${i})">${i}</a></li>`);
        }
    }

    if (currentPage < totalPages) {
        pagination.append(`<li class="page-item"><a class="page-link" href="#" id="nextPage">Next</a></li>`);
    } else {
        pagination.append(`<li class="page-item disabled"><a class="page-link" href="#" tabindex="-1" aria-disabled="true">Next</a></li>`);
    }

    $('#prevPage').click(() => {
        if (currentPage > 1) {
            currentPage--;
            listKinds(currentPage);
        }
    });

    $('#nextPage').click(() => {
        if (currentPage < totalPages) {
            currentPage++;
            listKinds(currentPage);
        }
    });
}

function goToPage(page) {
    currentPage = page;
    listKinds(currentPage);
}

function showError(txt) {
    var alertBox = $('<div class="alert alert-danger alert-dismissible fade in" role="alert">' +
                           '<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
                           txt +
                         '</div>');

    $('#alertContainer').empty().append(alertBox);

    setTimeout(function() {
        alertBox.alert('close');
    }, 3000); // Automatically hide after 3 seconds.
}

function showInfo(txt) {
    var alertBox = $('<div class="alert alert-info alert-dismissible fade in" role="alert">' +
                           '<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
                           txt +
                         '</div>');

    $('#alertContainer').empty().append(alertBox);

    setTimeout(function() {
        alertBox.alert('close');
    }, 3000); // Automatically hide after 3 seconds.
}

function sampleCoze() {
    $('#story').val(`{
    "uid": "simonzhou",
    "kindType": "CozeDemo",
    "param": {
        "summary": "Graceful music flows gently through the spacious hall, and the ball is in full swing."
    }
}`);
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#createBtn" ).click(() => generate());
    $( "#sampleCozeBtn" ).click(() => sampleCoze());
    $( "#searchBtn" ).click(() => searchKind());
    $( "#detailBtn" ).click(() => kindDetail());
    $( "#regenerateBtn" ).click(() => regenerate());
    $( "#listSearchBtn" ).click(() => {
        currentPage = 1;
        listKinds();
    });
    // popup control
    $('.close-button').click(function() {
        $('#popup').hide();
    });
    $(window).click(function(event) {
        if ($(event.target).is('#popup')) {
            $('#popup').hide();
        }
    });
});